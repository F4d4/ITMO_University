#include <stdio.h>
#include <string.h>

#include "app_main.h"
#include "app_config.h"
#include "board_pins.h"
#include "gpio_sim.h"
#include "sim_stimulus.h"
#include "sim_time.h"
#include "sim_trace.h"
#include "mygpio.h"
#include "naive_gpio.h"

/*
 * Ведущая программа симулятора.
 *
 * Модель GPIO запускается вместе с НАСТОЯЩИМИ исходными текстами программы
 * для стенда: mygpio, led, button, планировщик и прикладной автомат здесь те
 * же самые файлы, что компилируются в прошивку. Поэтому прогон воспроизводит
 * не пересказ логики задания, а саму логику.
 *
 * Прикладной уровень ничего не знает о симуляторе: события восстанавливаются
 * наблюдением за физическими выводами, как это делал бы логический анализатор.
 */

#define SIM_MAX_EVENTS  512u
#define SIM_MAX_PULSES   64u

typedef struct {
    uint32_t t_on;
    uint32_t t_off;
    uint8_t  color;     /* для двухцветного: 1 - жёлтый, 2 - красный */
} sim_pulse_t;

static stim_event_t b_stim[SIM_MAX_EVENTS];
static size_t       b_stim_count;

static uint8_t      b_btn_port;
static uint8_t      b_led_port;

static sim_pulse_t  b_green[SIM_MAX_PULSES];
static uint8_t      b_green_count;
static bool         b_green_on;

static sim_pulse_t  b_bi[SIM_MAX_PULSES];
static uint8_t      b_bi_count;
static uint8_t      b_bi_color;

static unsigned     b_failures;

#define SIM_EXPECT(cond, ...)                         \
    do {                                              \
        if (!(cond)) {                                \
            printf("  ОШИБКА: " __VA_ARGS__);         \
            printf("\n");                             \
            b_failures++;                             \
        }                                             \
    } while (0)

static void stim_add(uint32_t t_ms, pad_drive_t drive, const char *note)
{
    if (b_stim_count >= SIM_MAX_EVENTS) {
        return;
    }

    b_stim[b_stim_count].t_ms  = t_ms;
    b_stim[b_stim_count].port  = b_btn_port;
    b_stim[b_stim_count].pin   = BOARD_BTN_PIN;
    b_stim[b_stim_count].drive = drive;
    b_stim[b_stim_count].note  = note;
    b_stim_count++;
}

/*
 * Нажатие кнопки с дребезгом контактов: по нескольку ложных фронтов в первые
 * миллисекунды замыкания и размыкания. Аппаратного подавления дребезга в
 * стенде нет, поэтому эти фронты обязана отбросить программа.
 *
 * Обе пачки дребезга одинаковой длительности, поэтому измеренная программой
 * длительность нажатия в точности равна заданной здесь.
 */
static void stim_press(uint32_t t_down, uint32_t duration_ms)
{
    const uint32_t t_up = t_down + duration_ms;

    stim_add(t_down + 0u, PAD_LOW, "замыкание кнопки");
    stim_add(t_down + 1u, PAD_Z,   "дребезг");
    stim_add(t_down + 2u, PAD_LOW, "дребезг");
    stim_add(t_down + 4u, PAD_Z,   "дребезг");
    stim_add(t_down + 6u, PAD_LOW, "контакт установился");

    stim_add(t_up + 0u, PAD_Z,   "размыкание кнопки");
    stim_add(t_up + 1u, PAD_LOW, "дребезг");
    stim_add(t_up + 3u, PAD_Z,   "дребезг");
    stim_add(t_up + 5u, PAD_LOW, "дребезг");
    stim_add(t_up + 6u, PAD_Z,   "контакт разомкнулся");
}

static bool sim_pad_high(uint8_t port, uint8_t pin)
{
    return gpio_sim_pad_level(port, pin) == PAD_LEVEL_HIGH;
}

static bool sim_green_is_on(void)
{
    const bool high = sim_pad_high(b_led_port, BOARD_LED_GREEN_PIN);

    return (BOARD_LED_GREEN_ACTIVE_LEVEL != 0u) ? high : !high;
}

/* Цвет двухцветного светодиода определяется направлением тока через него */
static uint8_t sim_bicolor(void)
{
    const bool a = sim_pad_high(b_led_port, BOARD_LED_BI_A_PIN);
    const bool b = sim_pad_high(b_led_port, BOARD_LED_BI_B_PIN);

    if (a && !b) {
        return 1u;   /* жёлтый */
    }
    if (!a && b) {
        return 2u;   /* красный */
    }
    return 0u;       /* погашен */
}

static void sim_observe(void)
{
    const uint32_t now = sim_time_now();

    const bool green = sim_green_is_on();
    if (green != b_green_on) {
        b_green_on = green;

        if (green) {
            if (b_green_count < SIM_MAX_PULSES) {
                b_green[b_green_count].t_on  = now;
                b_green[b_green_count].t_off = 0u;
                b_green[b_green_count].color = 0u;
            }
            sim_trace_app("зелёный светодиод включён");
        } else {
            if (b_green_count < SIM_MAX_PULSES) {
                b_green[b_green_count].t_off = now;
                b_green_count++;
            }
            sim_trace_app("зелёный светодиод выключен");
        }
    }

    const uint8_t color = sim_bicolor();
    if (color != b_bi_color) {
        if ((b_bi_color != 0u) && (b_bi_count < SIM_MAX_PULSES)) {
            b_bi[b_bi_count].t_off = now;
            b_bi_count++;
        }
        if (color != 0u) {
            if (b_bi_count < SIM_MAX_PULSES) {
                b_bi[b_bi_count].t_on  = now;
                b_bi[b_bi_count].t_off = 0u;
                b_bi[b_bi_count].color = color;
            }
            sim_trace_app("двухцветный светодиод: %s",
                          (color == 1u) ? "жёлтый" : "красный");
        } else {
            sim_trace_app("двухцветный светодиод погашен");
        }
        b_bi_color = color;
    }
}

static void sim_run(uint32_t duration_ms)
{
    app_setup();

    b_green_on = sim_green_is_on();
    b_bi_color = sim_bicolor();

    for (uint32_t t = 0u; t <= duration_ms; ++t) {
        stim_apply_until(t);
        app_step();
        sim_observe();
        sim_time_advance(1u);
    }
}

static uint32_t sim_pulse_len(const sim_pulse_t *p)
{
    return p->t_off - p->t_on;
}

static void sim_check_close(const char *what, uint32_t actual, uint32_t expected, uint32_t tolerance)
{
    const uint32_t diff = (actual > expected) ? (actual - expected) : (expected - actual);

    SIM_EXPECT(diff <= tolerance, "%s: получено %u мс, ожидалось %u мс (допуск %u)",
               what, (unsigned)actual, (unsigned)expected, (unsigned)tolerance);
}

/* Ожидаемая длительность импульса зелёного светодиода для символа */
static uint32_t sim_mark_ms(char symbol)
{
    return (symbol == '.') ? (MORSE_UNIT_MS * MORSE_DOT_UNITS)
                           : (MORSE_UNIT_MS * MORSE_DASH_UNITS);
}

static void sim_check_playback(const char *expected)
{
    const uint8_t count = (uint8_t)strlen(expected);

    SIM_EXPECT(b_green_count == count,
               "число импульсов зелёного: получено %u, ожидалось %u",
               (unsigned)b_green_count, (unsigned)count);

    const uint8_t limit = (b_green_count < count) ? b_green_count : count;

    for (uint8_t i = 0u; i < limit; ++i) {
        char label[64];

        snprintf(label, sizeof(label), "импульс %u (%c)", (unsigned)(i + 1u), expected[i]);
        sim_check_close(label, sim_pulse_len(&b_green[i]), sim_mark_ms(expected[i]), 1u);

        if (i > 0u) {
            snprintf(label, sizeof(label), "межэлементный интервал %u", (unsigned)i);
            sim_check_close(label,
                            b_green[i].t_on - b_green[i - 1u].t_off,
                            (uint32_t)MORSE_UNIT_MS * MORSE_GAP_UNITS, 1u);
        }
    }
}

static void sim_check_feedback(const char *expected)
{
    const uint8_t count = (uint8_t)strlen(expected);

    SIM_EXPECT(b_bi_count == count,
               "число подтверждений ввода: получено %u, ожидалось %u",
               (unsigned)b_bi_count, (unsigned)count);

    const uint8_t limit = (b_bi_count < count) ? b_bi_count : count;

    for (uint8_t i = 0u; i < limit; ++i) {
        const uint8_t want = (expected[i] == '.') ? 1u : 2u;
        char          label[64];

        SIM_EXPECT(b_bi[i].color == want,
                   "подтверждение %u: цвет %u, ожидался %u (%c)",
                   (unsigned)(i + 1u), (unsigned)b_bi[i].color, (unsigned)want, expected[i]);

        snprintf(label, sizeof(label), "длительность подтверждения %u", (unsigned)(i + 1u));
        sim_check_close(label, sim_pulse_len(&b_bi[i]), MORSE_FEEDBACK_MS, 1u);
    }
}

/* Построить ввод последовательности символов нажатиями кнопки */
static uint32_t sim_build_input(const char *symbols, uint32_t t_start)
{
    uint32_t t = t_start;

    for (const char *s = symbols; *s != '\0'; ++s) {
        const uint32_t duration = (*s == '.') ? 120u : 500u;

        stim_press(t, duration);
        t += duration + 400u;   /* пауза заведомо короче тайм-аута конца ввода */
    }
    return t;
}

static int sim_scenario_sequence(const char *name, const char *input,
                                 const char *played, const char *feedback)
{
    printf("Сценарий \"%s\": ввод %s\n", name, input);

    const uint32_t t_end = sim_build_input(input, 1000u) + MORSE_INPUT_TIMEOUT_MS + 12000u;

    stim_load(b_stim, b_stim_count);
    sim_run(t_end);

    sim_check_feedback(feedback);
    sim_check_playback(played);

    SIM_EXPECT(gpio_sim_violations() == 0u,
               "модель обнаружила нарушений: %u", (unsigned)gpio_sim_violations());

    printf("  подтверждений ввода: %u, импульсов передачи: %u, нарушений: %u\n",
           (unsigned)b_bi_count, (unsigned)b_green_count,
           (unsigned)gpio_sim_violations());
    return 0;
}

int main(int argc, char **argv)
{
    const char *scenario = (argc > 1) ? argv[1] : "morse";
    uint32_t    offset   = 0u;
    char        log_path[256];
    char        js_path[256];

    sim_time_reset();
    gpio_sim_reset();

    if (!gpio_sim_decode((uintptr_t)BOARD_BTN_PORT, &b_btn_port, &offset) ||
        !gpio_sim_decode((uintptr_t)BOARD_LED_PORT, &b_led_port, &offset)) {
        printf("не удалось определить порты по карте выводов\n");
        return 2;
    }

    snprintf(log_path, sizeof(log_path), "sim/build/trace_%s.log", scenario);
    snprintf(js_path,  sizeof(js_path),  "sim/web/trace_%s.js",    scenario);

    if (!sim_trace_open(log_path, js_path, scenario)) {
        printf("не удалось открыть файлы журнала\n");
        return 2;
    }

    if (strcmp(scenario, "morse") == 0) {
        /* Восемь символов: три точки, три тире, две точки */
        (void)sim_scenario_sequence("morse", "...---..", "...---..", "...---..");
    } else if (strcmp(scenario, "overflow") == 0) {
        /*
         * Девять нажатий при ёмкости буфера восемь. Девятое не запоминается,
         * о чём сообщает частое мигание красным, а передаётся ровно восемь
         * элементов.
         */
        printf("Сценарий \"overflow\": девять нажатий при ёмкости %u\n",
               (unsigned)MORSE_CAPACITY);

        const uint32_t t_end = sim_build_input(".........", 1000u)
                             + MORSE_INPUT_TIMEOUT_MS + 12000u;

        stim_load(b_stim, b_stim_count);
        sim_run(t_end);

        sim_check_playback("........");
        SIM_EXPECT(b_bi_count > 8u,
                   "после переполнения ожидалась индикация ошибки, подтверждений: %u",
                   (unsigned)b_bi_count);
        SIM_EXPECT(gpio_sim_violations() == 0u,
                   "модель обнаружила нарушений: %u", (unsigned)gpio_sim_violations());

        printf("  импульсов передачи: %u (ожидалось 8), индикаций: %u\n",
               (unsigned)b_green_count, (unsigned)b_bi_count);
    } else if (strcmp(scenario, "edge") == 0) {
        /* Граница классификации: 299 мс это точка, 301 мс это тире */
        printf("Сценарий \"edge\": нажатия 299 и 301 мс при пороге %u мс\n",
               (unsigned)MORSE_DASH_THRESHOLD_MS);

        stim_press(1000u, MORSE_DASH_THRESHOLD_MS - 1u);
        stim_press(2000u, MORSE_DASH_THRESHOLD_MS + 1u);

        stim_load(b_stim, b_stim_count);
        sim_run(2000u + MORSE_DASH_THRESHOLD_MS + MORSE_INPUT_TIMEOUT_MS + 8000u);

        sim_check_feedback(".-");
        sim_check_playback(".-");
        SIM_EXPECT(gpio_sim_violations() == 0u,
                   "модель обнаружила нарушений: %u", (unsigned)gpio_sim_violations());
    } else if (strcmp(scenario, "glitch") == 0) {
        /*
         * Сравнение порядка записи управляющих регистров при переводе вывода
         * в режим выхода, когда в защёлке ODR осталось значение от прежней
         * настройки.
         *
         * Правильный порядок загружает ODR, пока выходной каскад ещё выключен,
         * поэтому вывод делает ровно один переход в конечное состояние.
         * Наивный порядок включает каскад первым, и на выводе успевает
         * появиться старое содержимое защёлки - паразитный импульс, который
         * на стенде без осциллографа не увидеть.
         */
        printf("Сценарий \"glitch\": порядок записи регистров при инициализации\n");

        static const mygpio_cfg_t as_input = {
            .mode = MYGPIO_MODE_INPUT, .otype = MYGPIO_OTYPE_PP,
            .speed = MYGPIO_SPEED_LOW, .pull = MYGPIO_PULL_NONE,
            .af = 0u, .init_level = MYGPIO_LEVEL_KEEP
        };
        static const mygpio_cfg_t as_output_low = {
            .mode = MYGPIO_MODE_OUTPUT, .otype = MYGPIO_OTYPE_PP,
            .speed = MYGPIO_SPEED_LOW, .pull = MYGPIO_PULL_NONE,
            .af = 0u, .init_level = 0u
        };

        const uint8_t pin = BOARD_LED_GREEN_PIN;

        (void)mygpio_port_clock_enable(BOARD_LED_PORT);

        /* Исходные условия: вывод - вход, в защёлке ODR осталась единица */
        (void)mygpio_init_pin(BOARD_LED_PORT, pin, &as_input);
        mygpio_write_pin(BOARD_LED_PORT, pin, 1u);

        sim_trace_app("правильный порядок: ODR загружается до включения каскада");
        uint32_t before = gpio_sim_pad_transitions(b_led_port, pin);
        (void)mygpio_init_pin(BOARD_LED_PORT, pin, &as_output_low);
        const uint32_t correct = gpio_sim_pad_transitions(b_led_port, pin) - before;

        /* Возвращаем те же исходные условия */
        (void)mygpio_init_pin(BOARD_LED_PORT, pin, &as_input);
        mygpio_write_pin(BOARD_LED_PORT, pin, 1u);

        sim_trace_app("наивный порядок: MODER пишется первым");
        before = gpio_sim_pad_transitions(b_led_port, pin);
        naive_gpio_init_output(BOARD_LED_PORT, pin, 0u);
        const uint32_t naive = gpio_sim_pad_transitions(b_led_port, pin) - before;

        printf("  переходов на выводе P%c%u: правильный порядок %u, наивный %u\n",
               (char)('A' + b_led_port), (unsigned)pin,
               (unsigned)correct, (unsigned)naive);

        SIM_EXPECT(correct == 1u,
                   "правильный порядок дал %u переходов вместо одного", (unsigned)correct);
        SIM_EXPECT(naive == 2u,
                   "наивный порядок дал %u переходов, ожидался паразитный импульс",
                   (unsigned)naive);
    } else {
        printf("неизвестный сценарий: %s\n", scenario);
        sim_trace_close();
        return 2;
    }

    sim_trace_close();

    if (b_failures == 0u) {
        printf("Проверки пройдены.\n");
        return 0;
    }
    printf("Проверок провалено: %u\n", b_failures);
    return 1;
}
