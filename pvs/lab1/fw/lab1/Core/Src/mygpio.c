#include <stddef.h>

#include "mygpio.h"
#include "gpio_bus.h"

#define MYGPIO_PIN_COUNT  16u
#define MYGPIO_LCKR_LCKK  (1u << 16)

/*
 * Критическая секция. Операции "чтение-модификация-запись" над общими
 * регистрами порта не атомарны, а прерывание SysTick работает всегда.
 * В сборке симулятора прерываний нет, секция вырождается в пустышку.
 */
typedef uint32_t mygpio_crit_t;

#ifdef SIM_BUILD
static inline mygpio_crit_t mygpio_crit_enter(void)           { return 0u; }
static inline void          mygpio_crit_exit (mygpio_crit_t s){ (void)s;   }
#else
static inline mygpio_crit_t mygpio_crit_enter(void)
{
    const uint32_t saved = __get_PRIMASK();
    __disable_irq();
    return saved;
}
static inline void mygpio_crit_exit(mygpio_crit_t saved)
{
    __set_PRIMASK(saved);
}
#endif

/*
 * Сборка нового значения регистра с двухразрядными полями сразу для всей
 * маски. Отдельные "чтение-модификация-запись" на каждый вывод оставили бы
 * регистр в промежуточном, полусконфигурированном состоянии - именно это
 * задание запрещает. Здесь получается ровно одна запись в регистр.
 */
static uint32_t mygpio_field2(uint32_t reg, uint16_t mask, uint32_t value)
{
    uint32_t clear = 0u;
    uint32_t set   = 0u;

    for (uint8_t pin = 0u; pin < MYGPIO_PIN_COUNT; ++pin) {
        if ((mask & (uint16_t)(1u << pin)) != 0u) {
            clear |= 3u           << (pin * 2u);
            set   |= (value & 3u) << (pin * 2u);
        }
    }
    return (reg & ~clear) | set;
}

/* То же самое для регистра с одноразрядными полями (OTYPER) */
static uint32_t mygpio_field1(uint32_t reg, uint16_t mask, uint32_t value)
{
    return (value != 0u) ? (reg | (uint32_t)mask) : (reg & ~(uint32_t)mask);
}

/* Номер альтернативной функции: по одной записи в AFRL и AFRH */
static void mygpio_afr_write(GPIO_TypeDef *port, uint16_t mask, uint8_t af)
{
    uint32_t clear[2] = { 0u, 0u };
    uint32_t set  [2] = { 0u, 0u };

    for (uint8_t pin = 0u; pin < MYGPIO_PIN_COUNT; ++pin) {
        if ((mask & (uint16_t)(1u << pin)) != 0u) {
            const uint8_t idx   = (pin < 8u) ? 0u : 1u;
            const uint8_t shift = (uint8_t)((pin & 7u) * 4u);

            clear[idx] |= 0xFu                  << shift;
            set  [idx] |= ((uint32_t)af & 0xFu) << shift;
        }
    }

    for (uint8_t i = 0u; i < 2u; ++i) {
        if (clear[i] != 0u) {
            GPIO_AFR_WR(port, i, (GPIO_AFR_RD(port, i) & ~clear[i]) | set[i]);
        }
    }
}

mygpio_status_t mygpio_port_clock_enable(GPIO_TypeDef *port)
{
    if (port == NULL) {
        return MYGPIO_ERR_PARAM;
    }
    gpio_bus_clock_enable(port, 1);
    return MYGPIO_OK;
}

mygpio_status_t mygpio_port_clock_disable(GPIO_TypeDef *port)
{
    if (port == NULL) {
        return MYGPIO_ERR_PARAM;
    }
    gpio_bus_clock_enable(port, 0);
    return MYGPIO_OK;
}

uint16_t mygpio_locked_mask(GPIO_TypeDef *port)
{
    const uint32_t lckr = GPIO_REG_RD(port, LCKR);

    return ((lckr & MYGPIO_LCKR_LCKK) != 0u) ? (uint16_t)(lckr & 0xFFFFu) : 0u;
}

/*
 * Инициализация режима работы выводов порта.
 *
 * Порядок записи управляющих регистров выбран так, чтобы на выходных выводах
 * не возникало кратковременных переключений в некорректное состояние:
 *
 *   Правило 1 (переход В режим выхода или альтернативной функции).
 *     MODER пишется ПОСЛЕДНИМ. Как только MODER переводит вывод в режим
 *     выхода, выходной буфер подключается к площадке и немедленно выдаёт
 *     содержимое защёлки ODR. Поэтому сначала линия уводится в Hi-Z, затем
 *     предзагружается ODR и настраиваются OSPEEDR / OTYPER / PUPDR / AFR, и
 *     только после этого включается драйвер. Вывод переходит из Hi-Z сразу в
 *     конечное состояние одним чистым переходом.
 *
 *   Правило 2 (переход ИЗ режима выхода в режим входа или аналоговый).
 *     MODER пишется ПЕРВЫМ: драйвер отключается прежде, чем изменятся
 *     подтяжки. Иначе подтяжка вниз оказалась бы кратковременно приложена к
 *     выводу, всё ещё активно выдающему единицу.
 *
 *   Правило 3 (выход остаётся выходом, меняются только скорость, тип выхода
 *     или подтяжка). Проход через Hi-Z сам по себе был бы выбросом, поэтому
 *     MODER для таких выводов не трогается, уровень сохраняется, а остальные
 *     регистры меняются на ходу - эти записи драйвер не отключают.
 */
mygpio_status_t mygpio_init_mask(GPIO_TypeDef *port, uint16_t mask, const mygpio_cfg_t *cfg)
{
    if ((port == NULL) || (cfg == NULL)) {
        return MYGPIO_ERR_PARAM;
    }
    if (mask == 0u) {
        return MYGPIO_OK;
    }
    if (cfg->af > 15u) {
        return MYGPIO_ERR_PARAM;
    }
    if ((cfg->init_level > 1u) && (cfg->init_level != MYGPIO_LEVEL_KEEP)) {
        return MYGPIO_ERR_PARAM;
    }
    if ((mygpio_locked_mask(port) & mask) != 0u) {
        return MYGPIO_ERR_LOCKED;
    }

    const bool target_drives = (cfg->mode == MYGPIO_MODE_OUTPUT) ||
                               (cfg->mode == MYGPIO_MODE_AF);

    const mygpio_crit_t crit = mygpio_crit_enter();

    /* Какие выводы маски сейчас управляют линией и какие уже в нужном режиме */
    const uint32_t moder_now = GPIO_REG_RD(port, MODER);
    uint16_t driving   = 0u;
    uint16_t same_mode = 0u;

    for (uint8_t pin = 0u; pin < MYGPIO_PIN_COUNT; ++pin) {
        const uint16_t bit = (uint16_t)(1u << pin);

        if ((mask & bit) == 0u) {
            continue;
        }

        const uint32_t mode = (moder_now >> (pin * 2u)) & 3u;

        if ((mode == (uint32_t)MYGPIO_MODE_OUTPUT) || (mode == (uint32_t)MYGPIO_MODE_AF)) {
            driving |= bit;
        }
        if (mode == (uint32_t)cfg->mode) {
            same_mode |= bit;
        }
    }

    if (!target_drives) {
        /* Правило 2 */
        GPIO_REG_WR(port, MODER,   mygpio_field2(moder_now, mask, (uint32_t)cfg->mode));
        GPIO_REG_WR(port, OSPEEDR, mygpio_field2(GPIO_REG_RD(port, OSPEEDR), mask, (uint32_t)cfg->speed));
        GPIO_REG_WR(port, OTYPER,  mygpio_field1(GPIO_REG_RD(port, OTYPER),  mask, (uint32_t)cfg->otype));
        GPIO_REG_WR(port, PUPDR,   mygpio_field2(GPIO_REG_RD(port, PUPDR),   mask, (uint32_t)cfg->pull));
    } else {
        /* Правило 3: выводы, уже работающие в целевом режиме, через Hi-Z не проводим */
        const uint16_t park = (uint16_t)(driving & (uint16_t)~same_mode);

        if (park != 0u) {
            GPIO_REG_WR(port, MODER,
                        mygpio_field2(moder_now, park, (uint32_t)MYGPIO_MODE_INPUT));
        }

        if (cfg->init_level != MYGPIO_LEVEL_KEEP) {
            /*
             * Предзагрузка ODR при выключенном выходном буфере: запись в BSRR
             * меняет защёлку, но в режиме входа она к площадке не подключена,
             * поэтому на выводе ничего не появляется.
             */
            mygpio_write_mask(port, mask, (cfg->init_level != 0u) ? 0xFFFFu : 0x0000u);
        }

        GPIO_REG_WR(port, OSPEEDR, mygpio_field2(GPIO_REG_RD(port, OSPEEDR), mask, (uint32_t)cfg->speed));
        GPIO_REG_WR(port, OTYPER,  mygpio_field1(GPIO_REG_RD(port, OTYPER),  mask, (uint32_t)cfg->otype));
        GPIO_REG_WR(port, PUPDR,   mygpio_field2(GPIO_REG_RD(port, PUPDR),   mask, (uint32_t)cfg->pull));

        if (cfg->mode == MYGPIO_MODE_AF) {
            /* Номер функции выбирается до того, как мультиплексор будет подключён */
            mygpio_afr_write(port, mask, cfg->af);
        }

        /* Правило 1: единственная запись, подключающая драйвер к площадке */
        GPIO_REG_WR(port, MODER,
                    mygpio_field2(GPIO_REG_RD(port, MODER), mask, (uint32_t)cfg->mode));
    }

    mygpio_crit_exit(crit);
    return MYGPIO_OK;
}

mygpio_status_t mygpio_init_pin(GPIO_TypeDef *port, uint8_t pin, const mygpio_cfg_t *cfg)
{
    if (pin >= MYGPIO_PIN_COUNT) {
        return MYGPIO_ERR_PARAM;
    }
    return mygpio_init_mask(port, (uint16_t)(1u << pin), cfg);
}

mygpio_status_t mygpio_deinit_mask(GPIO_TypeDef *port, uint16_t mask)
{
    static const mygpio_cfg_t reset_cfg = {
        .mode       = MYGPIO_MODE_INPUT,
        .otype      = MYGPIO_OTYPE_PP,
        .speed      = MYGPIO_SPEED_LOW,
        .pull       = MYGPIO_PULL_NONE,
        .af         = 0u,
        .init_level = MYGPIO_LEVEL_KEEP
    };

    const mygpio_status_t status = mygpio_init_mask(port, mask, &reset_cfg);

    if (status == MYGPIO_OK) {
        const mygpio_crit_t crit = mygpio_crit_enter();
        mygpio_afr_write(port, mask, 0u);
        mygpio_crit_exit(crit);
    }
    return status;
}

mygpio_status_t mygpio_get_config(GPIO_TypeDef *port, uint8_t pin, mygpio_cfg_t *out)
{
    if ((port == NULL) || (out == NULL) || (pin >= MYGPIO_PIN_COUNT)) {
        return MYGPIO_ERR_PARAM;
    }

    const uint8_t shift2 = (uint8_t)(pin * 2u);
    const uint8_t idx    = (pin < 8u) ? 0u : 1u;
    const uint8_t shift4 = (uint8_t)((pin & 7u) * 4u);

    out->mode       = (mygpio_mode_t) ((GPIO_REG_RD(port, MODER)   >> shift2) & 3u);
    out->otype      = (mygpio_otype_t)((GPIO_REG_RD(port, OTYPER)  >> pin)    & 1u);
    out->speed      = (mygpio_speed_t)((GPIO_REG_RD(port, OSPEEDR) >> shift2) & 3u);
    out->pull       = (mygpio_pull_t) ((GPIO_REG_RD(port, PUPDR)   >> shift2) & 3u);
    out->af         = (uint8_t)((GPIO_AFR_RD(port, idx) >> shift4) & 0xFu);
    out->init_level = (uint8_t)((GPIO_REG_RD(port, ODR) >> pin)    & 1u);

    return MYGPIO_OK;
}

uint16_t mygpio_read_port(GPIO_TypeDef *port)
{
    return (uint16_t)GPIO_REG_RD(port, IDR);
}

uint16_t mygpio_read_mask(GPIO_TypeDef *port, uint16_t mask)
{
    return (uint16_t)(GPIO_REG_RD(port, IDR) & (uint32_t)mask);
}

uint8_t mygpio_read_pin(GPIO_TypeDef *port, uint8_t pin)
{
    return (uint8_t)((GPIO_REG_RD(port, IDR) >> pin) & 1u);
}

uint16_t mygpio_read_out_mask(GPIO_TypeDef *port, uint16_t mask)
{
    return (uint16_t)(GPIO_REG_RD(port, ODR) & (uint32_t)mask);
}

uint8_t mygpio_read_out_pin(GPIO_TypeDef *port, uint8_t pin)
{
    return (uint8_t)((GPIO_REG_RD(port, ODR) >> pin) & 1u);
}

/*
 * Одновременная запись нескольких сигналов порта.
 *
 * Все выводы маски меняются одной 32-разрядной записью в BSRR: половина
 * BS[15:0] устанавливает, половина BR[31:16] сбрасывает. Это не
 * "чтение-модификация-запись", поэтому операция атомарна относительно
 * прерываний и не может быть затёрта обработчиком, трогающим другой вывод
 * того же порта. Для двухцветного светодиода, у которого два вывода должны
 * менять уровни одновременно и в противофазе, это единственный способ не
 * получить промежуточный неверный цвет.
 */
void mygpio_write_mask(GPIO_TypeDef *port, uint16_t mask, uint16_t values)
{
    const uint32_t bsrr = (uint32_t)(uint16_t)(mask &  values)
                        | ((uint32_t)(uint16_t)(mask & (uint16_t)~values) << 16);

    GPIO_REG_WR(port, BSRR, bsrr);
}

void mygpio_write_pin(GPIO_TypeDef *port, uint8_t pin, uint8_t level)
{
    mygpio_write_mask(port, (uint16_t)(1u << pin), (level != 0u) ? 0xFFFFu : 0x0000u);
}

void mygpio_set_mask(GPIO_TypeDef *port, uint16_t mask)
{
    GPIO_REG_WR(port, BSRR, (uint32_t)mask);
}

void mygpio_clear_mask(GPIO_TypeDef *port, uint16_t mask)
{
    GPIO_REG_WR(port, BSRR, (uint32_t)mask << 16);
}

void mygpio_toggle_mask(GPIO_TypeDef *port, uint16_t mask)
{
    const uint16_t odr = (uint16_t)GPIO_REG_RD(port, ODR);

    mygpio_write_mask(port, mask, (uint16_t)~odr);
}

void mygpio_write_port(GPIO_TypeDef *port, uint16_t value)
{
    GPIO_REG_WR(port, ODR, (uint32_t)value);
}

/*
 * Блокировка конфигурации выводов до следующего сброса.
 * Последовательность ключа (RM0090): запись LCKK|mask, запись mask,
 * запись LCKK|mask, чтение, повторное чтение. Выполняется целиком в
 * критической секции - прерывание внутри последовательности сорвало бы её.
 */
mygpio_status_t mygpio_lock_mask(GPIO_TypeDef *port, uint16_t mask)
{
    if (port == NULL) {
        return MYGPIO_ERR_PARAM;
    }
    if (mask == 0u) {
        return MYGPIO_OK;
    }

    const uint32_t      value = (uint32_t)mask;
    const mygpio_crit_t crit  = mygpio_crit_enter();

    GPIO_REG_WR(port, LCKR, MYGPIO_LCKR_LCKK | value);
    GPIO_REG_WR(port, LCKR, value);
    GPIO_REG_WR(port, LCKR, MYGPIO_LCKR_LCKK | value);
    (void)GPIO_REG_RD(port, LCKR);

    const uint32_t check = GPIO_REG_RD(port, LCKR);

    mygpio_crit_exit(crit);

    return ((check & MYGPIO_LCKR_LCKK) != 0u) ? MYGPIO_OK : MYGPIO_ERR_PARAM;
}
