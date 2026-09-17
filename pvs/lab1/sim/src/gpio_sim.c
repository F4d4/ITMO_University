#include <stddef.h>
#include <string.h>

#include "gpio_sim.h"
#include "sim_trace.h"
#include "cmsis_shim.h"

#define SIM_LCKR_LCKK  (1u << 16)

typedef struct {
    uint32_t moder;
    uint32_t otyper;
    uint32_t ospeedr;
    uint32_t pupdr;
    uint32_t odr;
    uint32_t lckr;
    uint32_t afr[2];

    bool     clock_on;

    /* Состояние последовательности ключа блокировки конфигурации */
    uint8_t  lock_step;
    uint32_t lock_mask;
    bool     locked;

    /* Внешний мир и результирующие уровни на площадках */
    pad_drive_t ext[SIM_PIN_COUNT];
    pad_level_t level[SIM_PIN_COUNT];
    uint32_t    transitions[SIM_PIN_COUNT];
} sim_port_t;

static sim_port_t b_port[SIM_PORT_COUNT];
static uint32_t   b_violations;

static uint32_t sim_field2(uint32_t reg, uint8_t pin)
{
    return (reg >> (pin * 2u)) & 3u;
}

/*
 * Разрешение уровня на физическом выводе.
 *
 * На площадку одновременно могут воздействовать три источника: выходной
 * каскад микроконтроллера, внешняя цепь и подтягивающий резистор. Приоритет
 * соответствует реальной схеме: активный выходной каскад сильнее подтяжки, а
 * встречное воздействие внешней цепи и выходного каскада даёт конфликт.
 *
 * Режим альтернативной функции в этой работе не используется, поэтому
 * трактуется как высокоимпедансное состояние - это задокументированное
 * упрощение модели.
 */
static pad_level_t sim_resolve(const sim_port_t *p, uint8_t pin)
{
    const uint32_t mode = sim_field2(p->moder, pin);
    pad_drive_t    mcu  = PAD_Z;

    if (mode == 1u) {
        const bool open_drain = ((p->otyper >> pin) & 1u) != 0u;
        const bool bit        = ((p->odr    >> pin) & 1u) != 0u;

        if (!open_drain) {
            mcu = bit ? PAD_HIGH : PAD_LOW;
        } else if (!bit) {
            mcu = PAD_LOW;      /* открытый сток: единица оставляет вывод свободным */
        }
    }

    const pad_drive_t ext = p->ext[pin];

    if ((mcu != PAD_Z) && (ext != PAD_Z) && (mcu != ext)) {
        return PAD_LEVEL_X;
    }
    if (mcu != PAD_Z) {
        return (mcu == PAD_HIGH) ? PAD_LEVEL_HIGH : PAD_LEVEL_LOW;
    }
    if (ext != PAD_Z) {
        return (ext == PAD_HIGH) ? PAD_LEVEL_HIGH : PAD_LEVEL_LOW;
    }

    switch (sim_field2(p->pupdr, pin)) {
    case 1u:  return PAD_LEVEL_HIGH;
    case 2u:  return PAD_LEVEL_LOW;
    default:  return PAD_LEVEL_FLOAT;
    }
}

/* Входной регистр отражает состояние площадок; в аналоговом режиме бит равен нулю */
static uint32_t sim_compute_idr(const sim_port_t *p)
{
    uint32_t idr = 0u;

    for (uint8_t pin = 0u; pin < SIM_PIN_COUNT; ++pin) {
        if (sim_field2(p->moder, pin) == 3u) {
            continue;
        }
        if (sim_resolve(p, pin) == PAD_LEVEL_HIGH) {
            idr |= 1u << pin;
        }
    }
    return idr;
}

static void sim_refresh_pads(uint8_t port, const char *note)
{
    sim_port_t *p = &b_port[port];

    for (uint8_t pin = 0u; pin < SIM_PIN_COUNT; ++pin) {
        const pad_level_t level = sim_resolve(p, pin);

        if (level != p->level[pin]) {
            p->level[pin] = level;
            p->transitions[pin]++;
            sim_trace_pad(port, pin, level, note);

            if (level == PAD_LEVEL_X) {
                b_violations++;
                sim_trace_violation("конфликт источников на выводе P%c%u",
                                    (char)('A' + port), (unsigned)pin);
            }
        }
    }
}

/*
 * Для заблокированных выводов поля конфигурационных регистров сохраняют
 * прежнее значение: после успешной последовательности ключа запись в них
 * не действует до следующего сброса.
 */
static uint32_t sim_keep_locked(const sim_port_t *p, uint32_t old_value, uint32_t new_value,
                                uint8_t bits, uint8_t first_pin)
{
    if (!p->locked) {
        return new_value;
    }

    const uint16_t lock  = (uint16_t)(p->lckr & 0xFFFFu);
    const uint8_t  count = (uint8_t)(32u / bits);
    uint32_t       result = new_value;

    for (uint8_t i = 0u; i < count; ++i) {
        const uint8_t pin = (uint8_t)(first_pin + i);

        if (pin >= SIM_PIN_COUNT) {
            break;
        }
        if (((lock >> pin) & 1u) != 0u) {
            const uint32_t field = (((1u << bits) - 1u) << (i * bits));
            result = (result & ~field) | (old_value & field);
        }
    }
    return result;
}

static void sim_write_lckr(uint8_t port, uint32_t value)
{
    sim_port_t *p = &b_port[port];

    if (p->locked) {
        b_violations++;
        sim_trace_violation("запись в LCKR порта GPIO%c после блокировки",
                            (char)('A' + port));
        return;
    }

    const uint32_t mask = value & 0xFFFFu;
    const bool     lckk = (value & SIM_LCKR_LCKK) != 0u;

    switch (p->lock_step) {
    case 0u:
        if (lckk) {
            p->lock_mask = mask;
            p->lock_step = 1u;
        }
        break;

    case 1u:
        p->lock_step = (!lckk && (mask == p->lock_mask)) ? 2u : 0u;
        break;

    case 2u:
        if (lckk && (mask == p->lock_mask)) {
            p->locked    = true;
            p->lock_step = 0u;
            p->lckr      = SIM_LCKR_LCKK | p->lock_mask;
            return;
        }
        p->lock_step = 0u;
        break;

    default:
        p->lock_step = 0u;
        break;
    }

    p->lckr = mask;
}

void gpio_sim_reset(void)
{
    memset(b_port, 0, sizeof(b_port));
    b_violations = 0u;

    /*
     * Значения после сброса по RM0090. У портов A и B часть выводов занята
     * отладочным интерфейсом, поэтому их регистры сброшены не нулями.
     */
    b_port[0].moder   = 0xA8000000u;   /* GPIOA */
    b_port[0].ospeedr = 0x0C000000u;
    b_port[0].pupdr   = 0x64000000u;
    b_port[1].moder   = 0x00000280u;   /* GPIOB */
    b_port[1].ospeedr = 0x000000C0u;
    b_port[1].pupdr   = 0x00000100u;

    for (uint8_t port = 0u; port < SIM_PORT_COUNT; ++port) {
        for (uint8_t pin = 0u; pin < SIM_PIN_COUNT; ++pin) {
            b_port[port].ext[pin]   = PAD_Z;
            b_port[port].level[pin] = sim_resolve(&b_port[port], pin);
        }
    }
}

bool gpio_sim_decode(uintptr_t address, uint8_t *port, uint32_t *offset)
{
    if (address < (uintptr_t)GPIOA_BASE) {
        return false;
    }

    const uintptr_t delta = address - (uintptr_t)GPIOA_BASE;
    const uint32_t  index = (uint32_t)(delta / 0x400u);

    if (index >= SIM_PORT_COUNT) {
        return false;
    }

    *port   = (uint8_t)index;
    *offset = (uint32_t)(delta % 0x400u);
    return true;
}

void gpio_sim_clock_enable(uint8_t port, bool enable)
{
    if (port < SIM_PORT_COUNT) {
        b_port[port].clock_on = enable;
    }
}

bool gpio_sim_clock_enabled(uint8_t port)
{
    return (port < SIM_PORT_COUNT) && b_port[port].clock_on;
}

uint32_t gpio_sim_read_reg(uint8_t port, uint32_t offset)
{
    if (port >= SIM_PORT_COUNT) {
        return 0u;
    }

    sim_port_t *p = &b_port[port];

    if (!p->clock_on) {
        b_violations++;
        sim_trace_violation("чтение регистра порта GPIO%c при выключенном тактировании",
                            (char)('A' + port));
        return 0u;
    }

    uint32_t value = 0u;

    switch (offset) {
    case SIM_REG_MODER:   value = p->moder;             break;
    case SIM_REG_OTYPER:  value = p->otyper;            break;
    case SIM_REG_OSPEEDR: value = p->ospeedr;           break;
    case SIM_REG_PUPDR:   value = p->pupdr;             break;
    case SIM_REG_IDR:     value = sim_compute_idr(p);   break;
    case SIM_REG_ODR:     value = p->odr;               break;
    case SIM_REG_BSRR:    value = 0u;                   break;  /* только запись */
    case SIM_REG_LCKR:    value = p->lckr;              break;
    case SIM_REG_AFRL:    value = p->afr[0];            break;
    case SIM_REG_AFRH:    value = p->afr[1];            break;
    default:
        b_violations++;
        sim_trace_violation("чтение по неизвестному смещению 0x%02X порта GPIO%c",
                            (unsigned)offset, (char)('A' + port));
        return 0u;
    }

    sim_trace_reg('R', port, offset, value, value);
    return value;
}

void gpio_sim_write_reg(uint8_t port, uint32_t offset, uint32_t value)
{
    if (port >= SIM_PORT_COUNT) {
        return;
    }

    sim_port_t *p = &b_port[port];

    if (!p->clock_on) {
        b_violations++;
        sim_trace_violation("запись в регистр порта GPIO%c при выключенном тактировании",
                            (char)('A' + port));
        return;
    }

    uint32_t    old_value = 0u;
    const char *note      = NULL;

    switch (offset) {
    case SIM_REG_MODER:
        old_value = p->moder;
        p->moder  = sim_keep_locked(p, old_value, value, 2u, 0u);
        note      = "MODER";
        break;

    case SIM_REG_OTYPER:
        old_value = p->otyper;
        p->otyper = sim_keep_locked(p, old_value, value & 0xFFFFu, 1u, 0u);
        note      = "OTYPER";
        break;

    case SIM_REG_OSPEEDR:
        old_value  = p->ospeedr;
        p->ospeedr = sim_keep_locked(p, old_value, value, 2u, 0u);
        note       = "OSPEEDR";
        break;

    case SIM_REG_PUPDR:
        old_value = p->pupdr;
        p->pupdr  = sim_keep_locked(p, old_value, value, 2u, 0u);
        note      = "PUPDR";
        break;

    case SIM_REG_IDR:
        b_violations++;
        sim_trace_violation("попытка записи в регистр IDR порта GPIO%c (только чтение)",
                            (char)('A' + port));
        return;

    case SIM_REG_ODR:
        old_value = p->odr;
        p->odr    = value & 0xFFFFu;
        note      = "ODR";
        break;

    case SIM_REG_BSRR: {
        const uint32_t set   = value & 0xFFFFu;
        const uint32_t reset = (value >> 16) & 0xFFFFu;

        old_value = p->odr;
        /* Если для вывода взведены оба бита, установка имеет приоритет над сбросом */
        p->odr    = (p->odr | set) & ~(reset & ~set);
        note      = "BSRR";
        sim_trace_reg('W', port, offset, value, old_value);
        sim_refresh_pads(port, note);
        return;
    }

    case SIM_REG_LCKR:
        old_value = p->lckr;
        sim_write_lckr(port, value);
        sim_trace_reg('W', port, offset, p->lckr, old_value);
        return;

    case SIM_REG_AFRL:
        old_value  = p->afr[0];
        p->afr[0]  = sim_keep_locked(p, old_value, value, 4u, 0u);
        note       = "AFRL";
        break;

    case SIM_REG_AFRH:
        old_value  = p->afr[1];
        p->afr[1]  = sim_keep_locked(p, old_value, value, 4u, 8u);
        note       = "AFRH";
        break;

    default:
        b_violations++;
        sim_trace_violation("запись по неизвестному смещению 0x%02X порта GPIO%c",
                            (unsigned)offset, (char)('A' + port));
        return;
    }

    sim_trace_reg('W', port, offset, value, old_value);
    sim_refresh_pads(port, note);
}

uint32_t gpio_sim_read32(uintptr_t address)
{
    uint8_t  port   = 0u;
    uint32_t offset = 0u;

    if (!gpio_sim_decode(address, &port, &offset)) {
        return 0u;
    }
    return gpio_sim_read_reg(port, offset);
}

void gpio_sim_write32(uintptr_t address, uint32_t value)
{
    uint8_t  port   = 0u;
    uint32_t offset = 0u;

    if (gpio_sim_decode(address, &port, &offset)) {
        gpio_sim_write_reg(port, offset, value);
    }
}

void gpio_sim_set_pad(uint8_t port, uint8_t pin, pad_drive_t drive, const char *note)
{
    if ((port >= SIM_PORT_COUNT) || (pin >= SIM_PIN_COUNT)) {
        return;
    }

    b_port[port].ext[pin] = drive;
    sim_refresh_pads(port, (note != NULL) ? note : "stimulus");
}

pad_level_t gpio_sim_pad_level(uint8_t port, uint8_t pin)
{
    if ((port >= SIM_PORT_COUNT) || (pin >= SIM_PIN_COUNT)) {
        return PAD_LEVEL_FLOAT;
    }
    return b_port[port].level[pin];
}

uint32_t gpio_sim_violations(void)
{
    return b_violations;
}

uint32_t gpio_sim_pad_transitions(uint8_t port, uint8_t pin)
{
    if ((port >= SIM_PORT_COUNT) || (pin >= SIM_PIN_COUNT)) {
        return 0u;
    }
    return b_port[port].transitions[pin];
}
