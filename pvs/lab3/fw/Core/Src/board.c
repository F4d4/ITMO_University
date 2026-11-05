#include "board.h"
#include "board_pins.h"
#include "gpio_hw.h"
#include "stm32f4xx_hal.h"

/*
 * Таблица "логический цвет канала -> (маска выводов, уровни на них)".
 *
 * Это единственное место, где закодирована электрическая схема индикации.
 *
 * Для двухцветного светодиода, включённого встречно-параллельно, цвет задаётся
 * направлением тока: один вывод в единице, другой в нуле. Комбинация "оба в
 * нуле" гасит прибор. Оба вывода всегда остаются выходами - на это прямо
 * указывает предупреждение учебного пособия о том, что второй контакт
 * необходимо сохранять выходом с нулём, а не оставлять неуправляемым.
 */
typedef struct {
    uint16_t mask;
    uint16_t values;
} board_led_pattern_t;

#define GREEN_ON_VALUES   ((BOARD_LED_GREEN_ACTIVE_LEVEL != 0u) ? BOARD_LED_GREEN_MASK : 0u)
#define GREEN_OFF_VALUES  ((BOARD_LED_GREEN_ACTIVE_LEVEL != 0u) ? 0u : BOARD_LED_GREEN_MASK)

static const board_led_pattern_t b_led_patterns[LED_CH_COUNT][LED_COLOR_COUNT] = {
    [LED_CH_GREEN] = {
        [LED_OFF] = { BOARD_LED_GREEN_MASK, GREEN_OFF_VALUES },
        [LED_ON]  = { BOARD_LED_GREEN_MASK, GREEN_ON_VALUES  },
        /* у одноцветного канала третьего состояния нет, трактуем как выключение */
        [LED_RED] = { BOARD_LED_GREEN_MASK, GREEN_OFF_VALUES }
    },
    [LED_CH_BICOLOR] = {
        [LED_OFF]    = { BOARD_LED_BI_MASK, 0u                  },
        [LED_YELLOW] = { BOARD_LED_BI_MASK, BOARD_LED_BI_A_MASK },
        [LED_RED]    = { BOARD_LED_BI_MASK, BOARD_LED_BI_B_MASK }
    }
};

void board_init(void)
{
    /* Счётчик тактов ядра: короткие паузы и замер длительности обменов I2C */
    CoreDebug->DEMCR |= CoreDebug_DEMCR_TRCENA_Msk;
    DWT->CYCCNT       = 0u;
    DWT->CTRL        |= DWT_CTRL_CYCCNTENA_Msk;

    gpio_hw_port_enable(BOARD_LED_PORT);
    gpio_hw_port_enable(BOARD_BTN_PORT);

    /*
     * Светодиоды инициализируются сразу погашенными: уровень задаётся до
     * включения выходного буфера, поэтому при старте программы на выводах
     * не возникает кратковременной вспышки.
     */
    gpio_hw_config_mask(BOARD_LED_PORT, BOARD_LED_ALL_MASK,
                        GPIO_HW_OUT_PP, GPIO_HW_NOPULL,
                        (GREEN_OFF_VALUES != 0u) ? 1u : 0u);

    board_led_apply(LED_CH_GREEN,   LED_OFF);
    board_led_apply(LED_CH_BICOLOR, LED_OFF);

    gpio_hw_config_mask(BOARD_BTN_PORT, BOARD_BTN_MASK,
                        GPIO_HW_IN,
                        (BOARD_BTN_ACTIVE_LEVEL == 0u) ? GPIO_HW_PULLUP : GPIO_HW_PULLDOWN,
                        GPIO_HW_LEVEL_KEEP);
}

void board_led_apply(led_ch_t ch, led_color_t color)
{
    if ((ch >= LED_CH_COUNT) || (color >= LED_COLOR_COUNT)) {
        return;
    }

    const board_led_pattern_t *pattern = &b_led_patterns[ch][color];

    /* Одна запись: оба вывода двухцветного светодиода меняются одновременно */
    gpio_hw_write_mask(BOARD_LED_PORT, pattern->mask, pattern->values);
}

bool board_button_is_closed(void)
{
    const uint16_t level = gpio_hw_read_mask(BOARD_BTN_PORT, BOARD_BTN_MASK);

    return (BOARD_BTN_ACTIVE_LEVEL == 0u) ? (level == 0u) : (level != 0u);
}

USART_TypeDef *board_uart_regs(void)
{
    return BOARD_UART;
}

IRQn_Type board_uart_irqn(void)
{
    return BOARD_UART_IRQn;
}

void board_uart_hw_init(void)
{
    BOARD_UART_CLK_ENABLE();

    gpio_hw_port_enable(BOARD_UART_PORT);
    gpio_hw_config_af(BOARD_UART_PORT, BOARD_UART_TX_MASK, BOARD_UART_AF, GPIO_HW_NOPULL);
    /*
     * Подтяжка на RX держит линию в состоянии покоя ("1"), пока отладчик не
     * подключён: иначе плавающий вход давал бы ложные старт-биты и ошибки кадра.
     */
    gpio_hw_config_af(BOARD_UART_PORT, BOARD_UART_RX_MASK, BOARD_UART_AF, GPIO_HW_PULLUP);
}

uint32_t board_uart_clock_hz(void)
{
    return BOARD_UART_CLK_HZ();
}

I2C_TypeDef *board_i2c_regs(void)
{
    return BOARD_I2C;
}

IRQn_Type board_i2c_ev_irqn(void)
{
    return BOARD_I2C_EV_IRQn;
}

IRQn_Type board_i2c_er_irqn(void)
{
    return BOARD_I2C_ER_IRQn;
}

void board_i2c_hw_init(void)
{
    BOARD_I2C_CLK_ENABLE();

    gpio_hw_port_enable(BOARD_I2C_PORT);
    gpio_hw_config_af_od(BOARD_I2C_PORT, BOARD_I2C_SCL_MASK | BOARD_I2C_SDA_MASK,
                         BOARD_I2C_AF, GPIO_HW_NOPULL);
}

/* Полпериода тактов восстановления: 5 мкс - частота 100 кГц, допустимая для всех ведомых */
#define I2C_RECOVER_HALF_US  5u
#define I2C_RECOVER_CLOCKS   9u

static bool board_i2c_sda_high(void)
{
    return gpio_hw_read_mask(BOARD_I2C_PORT, BOARD_I2C_SDA_MASK) != 0u;
}

void board_i2c_bus_recover(void)
{
    gpio_hw_port_enable(BOARD_I2C_PORT);

    /* Оба вывода - выходы с открытым стоком в отпущенном состоянии (единица) */
    gpio_hw_config_mask(BOARD_I2C_PORT, BOARD_I2C_SCL_MASK | BOARD_I2C_SDA_MASK,
                        GPIO_HW_OUT_OD, GPIO_HW_NOPULL, 1u);
    board_delay_us(I2C_RECOVER_HALF_US);

    for (uint32_t i = 0u; (i < I2C_RECOVER_CLOCKS) && !board_i2c_sda_high(); ++i) {
        gpio_hw_write_mask(BOARD_I2C_PORT, BOARD_I2C_SCL_MASK, 0u);
        board_delay_us(I2C_RECOVER_HALF_US);
        gpio_hw_write_mask(BOARD_I2C_PORT, BOARD_I2C_SCL_MASK, BOARD_I2C_SCL_MASK);
        board_delay_us(I2C_RECOVER_HALF_US);
    }

    /* STOP: SDA переходит из нуля в единицу при высоком SCL */
    gpio_hw_write_mask(BOARD_I2C_PORT, BOARD_I2C_SCL_MASK, 0u);
    board_delay_us(I2C_RECOVER_HALF_US);
    gpio_hw_write_mask(BOARD_I2C_PORT, BOARD_I2C_SDA_MASK, 0u);
    board_delay_us(I2C_RECOVER_HALF_US);
    gpio_hw_write_mask(BOARD_I2C_PORT, BOARD_I2C_SCL_MASK, BOARD_I2C_SCL_MASK);
    board_delay_us(I2C_RECOVER_HALF_US);
    gpio_hw_write_mask(BOARD_I2C_PORT, BOARD_I2C_SDA_MASK, BOARD_I2C_SDA_MASK);
    board_delay_us(I2C_RECOVER_HALF_US);
}

TIM_TypeDef *board_sched_tim_regs(void)
{
    return BOARD_SCHED_TIM;
}

IRQn_Type board_sched_tim_irqn(void)
{
    return BOARD_SCHED_TIM_IRQn;
}

void board_sched_tim_clk_enable(void)
{
    BOARD_SCHED_TIM_CLK_ENABLE();
}

uint32_t board_sched_tim_clock_hz(void)
{
    /*
     * TIM6 сидит на APB1. Если делитель APB1 не равен 1, таймеры тактируются
     * удвоенной частотой шины (RM0090, раздел 6.2): 2 * 45 = 90 МГц.
     */
    const uint32_t pclk1 = HAL_RCC_GetPCLK1Freq();

    return ((RCC->CFGR & RCC_CFGR_PPRE1) == RCC_CFGR_PPRE1_DIV1) ? pclk1 : 2u * pclk1;
}

uint32_t board_cycles(void)
{
    return DWT->CYCCNT;
}

uint32_t board_cycles_per_us(void)
{
    return SystemCoreClock / 1000000u;
}

void board_delay_us(uint32_t us)
{
    const uint32_t start = DWT->CYCCNT;
    const uint32_t ticks = us * board_cycles_per_us();

    while ((uint32_t)(DWT->CYCCNT - start) < ticks) {
    }
}
