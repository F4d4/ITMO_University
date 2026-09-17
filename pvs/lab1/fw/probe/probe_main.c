#include "stm32f4xx_hal.h"

#include "board_pins.h"
#include "mygpio.h"
#include "sys_tick.h"

/*
 * Проба полярности индикации стенда.
 *
 * Отдельная маленькая программа, не входящая в состав лабораторной работы.
 * Она нужна один раз, чтобы достоверно установить:
 *   - каким уровнем зажигается зелёный светодиод;
 *   - какой цвет даёт каждая из двух комбинаций на выводах двухцветного;
 *   - гаснет ли двухцветный светодиод, когда оба его вывода в одном уровне
 *     (это подтверждает встречно-параллельное включение);
 *   - каким уровнем замкнутая кнопка тянет свой вывод.
 *
 * Результат наблюдения переносится в board_pins.h и в таблицу цветов
 * в board.c - больше нигде полярность не закодирована.
 */

#define PROBE_STEP_MS  2000u
#define PROBE_STEPS       8u

/* Комбинации уровней на выводах PD13, PD14, PD15 */
static const uint16_t b_pattern[PROBE_STEPS] = {
    0u,
    BOARD_LED_GREEN_MASK,
    BOARD_LED_BI_A_MASK,
    BOARD_LED_BI_B_MASK,
    BOARD_LED_BI_A_MASK | BOARD_LED_BI_B_MASK,
    BOARD_LED_GREEN_MASK | BOARD_LED_BI_A_MASK | BOARD_LED_BI_B_MASK,
    BOARD_LED_GREEN_MASK | BOARD_LED_BI_B_MASK,
    BOARD_LED_GREEN_MASK | BOARD_LED_BI_A_MASK
};

/* Доступно в отладчике: текущий шаг и уровень на выводе кнопки */
volatile uint32_t g_probe_step;
volatile uint32_t g_button_raw;

static void Error_Handler(void)
{
    __disable_irq();
    for (;;) {
    }
}

static void SystemClock_Config(void)
{
    RCC_OscInitTypeDef osc = { 0 };
    RCC_ClkInitTypeDef clk = { 0 };

    __HAL_RCC_PWR_CLK_ENABLE();
    __HAL_PWR_VOLTAGESCALING_CONFIG(PWR_REGULATOR_VOLTAGE_SCALE1);

    osc.OscillatorType = RCC_OSCILLATORTYPE_HSE;
    osc.HSEState       = RCC_HSE_ON;
    osc.PLL.PLLState   = RCC_PLL_ON;
    osc.PLL.PLLSource  = RCC_PLLSOURCE_HSE;
    osc.PLL.PLLM       = 25;
    osc.PLL.PLLN       = 360;
    osc.PLL.PLLP       = RCC_PLLP_DIV2;
    osc.PLL.PLLQ       = 8;

    if (HAL_RCC_OscConfig(&osc) != HAL_OK) {
        Error_Handler();
    }
    if (HAL_PWREx_EnableOverDrive() != HAL_OK) {
        Error_Handler();
    }

    clk.ClockType      = RCC_CLOCKTYPE_HCLK  | RCC_CLOCKTYPE_SYSCLK |
                         RCC_CLOCKTYPE_PCLK1 | RCC_CLOCKTYPE_PCLK2;
    clk.SYSCLKSource   = RCC_SYSCLKSOURCE_PLLCLK;
    clk.AHBCLKDivider  = RCC_SYSCLK_DIV1;
    clk.APB1CLKDivider = RCC_HCLK_DIV4;
    clk.APB2CLKDivider = RCC_HCLK_DIV2;

    if (HAL_RCC_ClockConfig(&clk, FLASH_LATENCY_5) != HAL_OK) {
        Error_Handler();
    }
}

int main(void)
{
    static const mygpio_cfg_t out_cfg = {
        .mode = MYGPIO_MODE_OUTPUT, .otype = MYGPIO_OTYPE_PP,
        .speed = MYGPIO_SPEED_LOW,  .pull  = MYGPIO_PULL_NONE,
        .af = 0u, .init_level = 0u
    };
    static const mygpio_cfg_t in_cfg = {
        .mode = MYGPIO_MODE_INPUT,  .otype = MYGPIO_OTYPE_PP,
        .speed = MYGPIO_SPEED_LOW,  .pull  = MYGPIO_PULL_UP,
        .af = 0u, .init_level = MYGPIO_LEVEL_KEEP
    };

    HAL_Init();
    SystemClock_Config();

    (void)mygpio_port_clock_enable(BOARD_LED_PORT);
    (void)mygpio_port_clock_enable(BOARD_BTN_PORT);
    (void)mygpio_init_mask(BOARD_LED_PORT, BOARD_LED_ALL_MASK, &out_cfg);
    (void)mygpio_init_mask(BOARD_BTN_PORT, BOARD_BTN_MASK,     &in_cfg);

    uint32_t step  = 0u;
    uint32_t t0    = sys_tick_ms();
    bool     mirror = false;

    for (;;) {
        g_button_raw = mygpio_read_pin(BOARD_BTN_PORT, BOARD_BTN_PIN);

        if (mirror) {
            /*
             * Второй режим: зелёный светодиод повторяет состояние кнопки.
             * Позволяет убедиться в полярности кнопки без отладчика.
             */
            mygpio_write_mask(BOARD_LED_PORT, BOARD_LED_GREEN_MASK,
                              (g_button_raw == 0u) ? BOARD_LED_GREEN_MASK : 0u);

            if ((uint32_t)(sys_tick_ms() - t0) >= (PROBE_STEP_MS * 5u)) {
                mirror = false;
                step   = 0u;
                t0     = sys_tick_ms();
            }
            continue;
        }

        if ((uint32_t)(sys_tick_ms() - t0) >= PROBE_STEP_MS) {
            t0   = sys_tick_ms();
            step = step + 1u;

            if (step >= PROBE_STEPS) {
                mirror = true;
                mygpio_write_mask(BOARD_LED_PORT, BOARD_LED_ALL_MASK, 0u);
                continue;
            }
        }

        g_probe_step = step;
        mygpio_write_mask(BOARD_LED_PORT, BOARD_LED_ALL_MASK, b_pattern[step]);
    }
}
