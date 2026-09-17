#include <stdarg.h>
#include <stdio.h>
#include <string.h>

#include "sim_trace.h"
#include "sim_time.h"

/*
 * Журналирование работы модели.
 *
 * Пишутся два файла: trace.log - человекочитаемый текст, который включается
 * в отчёт текстом, а не снимком экрана, и trace.js - те же события в виде
 * массивов для графического интерфейса. Формат trace.js выбран так, чтобы
 * страница открывалась по file:// без веб-сервера.
 *
 * Чтения регистров, возвращающие то же значение, что и предыдущее чтение
 * того же регистра, в журнал не попадают: опрос кнопки обращается к IDR
 * каждую миллисекунду, и без этого фильтра журнал состоял бы почти целиком
 * из повторов. Все записи в регистры журналируются без исключений.
 */

#define SIM_REG_SLOTS  10u

static FILE    *b_log;
/*
 * Записи трёх видов накапливаются в отдельных временных файлах и собираются
 * в trace.js при закрытии: иначе они перемешались бы в одном массиве.
 */
static FILE    *b_js_regs;
static FILE    *b_js_pads;
static FILE    *b_js_app;
static char     b_js_path[512];
static char     b_scenario[128];
static uint32_t b_violations;
static bool     b_first_reg;
static bool     b_first_pad;
static bool     b_first_app;

static uint32_t b_last_read[SIM_PORT_COUNT][SIM_REG_SLOTS];
static bool     b_has_read [SIM_PORT_COUNT][SIM_REG_SLOTS];

static const char *sim_reg_name(uint32_t offset)
{
    switch (offset) {
    case SIM_REG_MODER:   return "MODER";
    case SIM_REG_OTYPER:  return "OTYPER";
    case SIM_REG_OSPEEDR: return "OSPEEDR";
    case SIM_REG_PUPDR:   return "PUPDR";
    case SIM_REG_IDR:     return "IDR";
    case SIM_REG_ODR:     return "ODR";
    case SIM_REG_BSRR:    return "BSRR";
    case SIM_REG_LCKR:    return "LCKR";
    case SIM_REG_AFRL:    return "AFRL";
    case SIM_REG_AFRH:    return "AFRH";
    default:              return "?";
    }
}

static const char *sim_mode_name(uint32_t value)
{
    static const char *const names[4] = { "IN", "OUT", "AF", "AN" };
    return names[value & 3u];
}

static const char *sim_pull_name(uint32_t value)
{
    static const char *const names[4] = { "--", "PU", "PD", "??" };
    return names[value & 3u];
}

static void sim_append(char *buf, size_t size, const char *fmt, ...)
{
    const size_t used = strlen(buf);

    if (used + 1u >= size) {
        return;
    }

    va_list args;
    va_start(args, fmt);
    (void)vsnprintf(buf + used, size - used, fmt, args);
    va_end(args);
}

/* Короткая расшифровка изменившихся полей регистра */
static void sim_decode(char *out, size_t size, uint8_t port, uint32_t offset,
                       uint32_t value, uint32_t old_value)
{
    const char letter = (char)('A' + port);

    out[0] = '\0';

    switch (offset) {
    case SIM_REG_MODER:
    case SIM_REG_OSPEEDR:
    case SIM_REG_PUPDR:
        for (uint8_t pin = 0u; pin < SIM_PIN_COUNT; ++pin) {
            const uint32_t now = (value     >> (pin * 2u)) & 3u;
            const uint32_t was = (old_value >> (pin * 2u)) & 3u;

            if (now == was) {
                continue;
            }
            if (offset == SIM_REG_MODER) {
                sim_append(out, size, "P%c%u=%s ", letter, pin, sim_mode_name(now));
            } else if (offset == SIM_REG_PUPDR) {
                sim_append(out, size, "P%c%u=%s ", letter, pin, sim_pull_name(now));
            } else {
                sim_append(out, size, "P%c%u=SPD%u ", letter, pin, (unsigned)now);
            }
        }
        break;

    case SIM_REG_OTYPER:
        for (uint8_t pin = 0u; pin < SIM_PIN_COUNT; ++pin) {
            const uint32_t now = (value >> pin) & 1u;

            if (now != ((old_value >> pin) & 1u)) {
                sim_append(out, size, "P%c%u=%s ", letter, pin, (now != 0u) ? "OD" : "PP");
            }
        }
        break;

    case SIM_REG_ODR:
        for (uint8_t pin = 0u; pin < SIM_PIN_COUNT; ++pin) {
            const uint32_t now = (value >> pin) & 1u;

            if (now != ((old_value >> pin) & 1u)) {
                sim_append(out, size, "P%c%u=%u ", letter, pin, (unsigned)now);
            }
        }
        break;

    case SIM_REG_BSRR:
        for (uint8_t pin = 0u; pin < SIM_PIN_COUNT; ++pin) {
            if (((value >> pin) & 1u) != 0u) {
                sim_append(out, size, "BS%u ", pin);
            }
        }
        for (uint8_t pin = 0u; pin < SIM_PIN_COUNT; ++pin) {
            if (((value >> (pin + 16u)) & 1u) != 0u) {
                sim_append(out, size, "BR%u ", pin);
            }
        }
        break;

    case SIM_REG_IDR:
        for (uint8_t pin = 0u; pin < SIM_PIN_COUNT; ++pin) {
            if (((value >> pin) & 1u) != 0u) {
                sim_append(out, size, "P%c%u ", letter, pin);
            }
        }
        break;

    case SIM_REG_LCKR:
        sim_append(out, size, "LCKK=%u mask=0x%04X",
                   (unsigned)((value >> 16) & 1u), (unsigned)(value & 0xFFFFu));
        break;

    default:
        break;
    }
}

static const char *sim_level_text(pad_level_t level)
{
    switch (level) {
    case PAD_LEVEL_LOW:   return "0";
    case PAD_LEVEL_HIGH:  return "1";
    case PAD_LEVEL_X:     return "X";
    case PAD_LEVEL_FLOAT:
    default:              return "Z";
    }
}

bool sim_trace_open(const char *log_path, const char *js_path, const char *scenario)
{
    char path[512];

    b_log        = fopen(log_path, "w");
    b_violations = 0u;
    b_first_reg  = true;
    b_first_pad  = true;
    b_first_app  = true;

    memset(b_has_read, 0, sizeof(b_has_read));
    snprintf(b_js_path,  sizeof(b_js_path),  "%s", js_path);
    snprintf(b_scenario, sizeof(b_scenario), "%s", scenario);

    snprintf(path, sizeof(path), "%s.regs.tmp", js_path);
    b_js_regs = fopen(path, "w+");
    snprintf(path, sizeof(path), "%s.pads.tmp", js_path);
    b_js_pads = fopen(path, "w+");
    snprintf(path, sizeof(path), "%s.app.tmp", js_path);
    b_js_app  = fopen(path, "w+");

    if ((b_log == NULL) || (b_js_regs == NULL) || (b_js_pads == NULL) || (b_js_app == NULL)) {
        return false;
    }

    fprintf(b_log, "Журнал работы модели блока GPIO STM32F427\n");
    fprintf(b_log, "Сценарий: %s\n", scenario);
    fprintf(b_log, "Время указано в секундах от начала прогона.\n\n");

    return true;
}

void sim_trace_reg(char rw, uint8_t port, uint32_t offset, uint32_t value, uint32_t old_value)
{
    if (b_log == NULL) {
        return;
    }

    const uint32_t slot = offset / 4u;

    if ((rw == 'R') && (slot < SIM_REG_SLOTS) && (port < SIM_PORT_COUNT)) {
        if (b_has_read[port][slot] && (b_last_read[port][slot] == value)) {
            return;
        }
        b_has_read [port][slot] = true;
        b_last_read[port][slot] = value;
    }

    char decoded[256];
    sim_decode(decoded, sizeof(decoded), port, offset, value, old_value);

    const uint32_t now = sim_time_now();

    if (rw == 'W') {
        fprintf(b_log, "[%5u.%03u] W GPIO%c.%-7s 0x%08X -> 0x%08X  %s\n",
                (unsigned)(now / 1000u), (unsigned)(now % 1000u),
                (char)('A' + port), sim_reg_name(offset),
                (unsigned)old_value, (unsigned)value, decoded);
    } else {
        fprintf(b_log, "[%5u.%03u] R GPIO%c.%-7s = 0x%08X              %s\n",
                (unsigned)(now / 1000u), (unsigned)(now % 1000u),
                (char)('A' + port), sim_reg_name(offset),
                (unsigned)value, decoded);
    }

    fprintf(b_js_regs, "%s    {t:%u,rw:\"%c\",port:\"%c\",reg:\"%s\",val:%u,old:%u}",
            b_first_reg ? "" : ",\n", (unsigned)now, rw,
            (char)('A' + port), sim_reg_name(offset),
            (unsigned)value, (unsigned)old_value);
    b_first_reg = false;
}

void sim_trace_pad(uint8_t port, uint8_t pin, pad_level_t level, const char *note)
{
    if (b_log == NULL) {
        return;
    }

    const uint32_t now = sim_time_now();

    fprintf(b_log, "[%5u.%03u] PAD P%c%-2u <- %s   %s\n",
            (unsigned)(now / 1000u), (unsigned)(now % 1000u),
            (char)('A' + port), pin, sim_level_text(level),
            (note != NULL) ? note : "");

    fprintf(b_js_pads, "%s    {t:%u,port:\"%c\",pin:%u,lvl:\"%s\",note:\"%s\"}",
            b_first_pad ? "" : ",\n", (unsigned)now,
            (char)('A' + port), pin, sim_level_text(level),
            (note != NULL) ? note : "");
    b_first_pad = false;
}

void sim_trace_app(const char *fmt, ...)
{
    if (b_log == NULL) {
        return;
    }

    char text[256];
    va_list args;

    va_start(args, fmt);
    (void)vsnprintf(text, sizeof(text), fmt, args);
    va_end(args);

    const uint32_t now = sim_time_now();

    fprintf(b_log, "[%5u.%03u] APP %s\n",
            (unsigned)(now / 1000u), (unsigned)(now % 1000u), text);

    fprintf(b_js_app, "%s    {t:%u,msg:\"%s\"}",
            b_first_app ? "" : ",\n", (unsigned)now, text);
    b_first_app = false;
}

void sim_trace_violation(const char *fmt, ...)
{
    char text[256];
    va_list args;

    va_start(args, fmt);
    (void)vsnprintf(text, sizeof(text), fmt, args);
    va_end(args);

    b_violations++;

    if (b_log != NULL) {
        const uint32_t now = sim_time_now();

        fprintf(b_log, "[%5u.%03u] !!! %s\n",
                (unsigned)(now / 1000u), (unsigned)(now % 1000u), text);
    }
}

uint32_t sim_trace_violation_count(void)
{
    return b_violations;
}

/* Перелить накопленный временный файл в собираемый trace.js */
static void sim_trace_spill(FILE *dst, FILE *src)
{
    char   buffer[4096];
    size_t count;

    rewind(src);
    while ((count = fread(buffer, 1u, sizeof(buffer), src)) > 0u) {
        (void)fwrite(buffer, 1u, count, dst);
    }
}

void sim_trace_close(void)
{
    if (b_js_regs != NULL) {
        FILE *js = fopen(b_js_path, "w");

        if (js != NULL) {
            /* Каждый сценарий кладётся в общий словарь, чтобы страница
               могла показывать их все и переключаться между ними */
            fprintf(js, "window.TRACES = window.TRACES || {};\n");
            fprintf(js, "window.TRACES[\"%s\"] = {\n", b_scenario);
            fprintf(js, "  meta: { scenario: \"%s\" },\n", b_scenario);

            fprintf(js, "  regs: [\n");
            sim_trace_spill(js, b_js_regs);
            fprintf(js, "\n  ],\n  pads: [\n");
            sim_trace_spill(js, b_js_pads);
            fprintf(js, "\n  ],\n  app: [\n");
            sim_trace_spill(js, b_js_app);
            fprintf(js, "\n  ]\n};\n");
            fclose(js);
        }

        fclose(b_js_regs);
        fclose(b_js_pads);
        fclose(b_js_app);
        b_js_regs = NULL;
        b_js_pads = NULL;
        b_js_app  = NULL;

        char path[512];
        snprintf(path, sizeof(path), "%s.regs.tmp", b_js_path);  remove(path);
        snprintf(path, sizeof(path), "%s.pads.tmp", b_js_path);  remove(path);
        snprintf(path, sizeof(path), "%s.app.tmp",  b_js_path);  remove(path);
    }

    if (b_log != NULL) {
        fclose(b_log);
        b_log = NULL;
    }
}
