#include "stm32f4xx_hal.h"
#include "app_main.h"

/*
 * Точка входа прошивки: инициализация HAL, настройка тактирования и передача
 * управления прикладной части. Деталей работы с выводами здесь нет.
 */

static void Error_Handler(void)
{
    __disable_irq();
    for (;;) {
    }
}

/*
 * Тактирование стенда SDK-1.1M с процессорным модулем STM32F427VIT6.
 *
 * На плате установлен кварцевый резонатор 25 МГц. Требуется получить
 * максимальную для этого микроконтроллера частоту 180 МГц:
 *
 *   f_PLLin = HSE / M   = 25 / 25   =   1 МГц   (допустимо 0,95...2,1 МГц)
 *   f_VCO   = f_PLLin*N = 1 * 360   = 360 МГц   (допустимо 100...432 МГц)
 *   SYSCLK  = f_VCO / P = 360 / 2   = 180 МГц   (максимум для F427)
 *
 * M = 25 - единственный делитель, при котором из 25 МГц получаются целые N и P.
 *
 * HCLK  = SYSCLK / 1 = 180 МГц (максимум 180)
 * PCLK1 = HCLK   / 4 =  45 МГц (максимум 45)
 * PCLK2 = HCLK   / 2 =  90 МГц (максимум 90)
 *
 * При 180 МГц обязателен режим повышенной производительности (over-drive):
 * без него регулятор в Scale 1 обеспечивает только 168 МГц. Задержка чтения
 * флеш-памяти при напряжении питания 3,3 В и 180 МГц составляет 5 тактов.
 *
 * Выход PLLQ при f_VCO = 360 МГц не даёт ровно 48 МГц, поэтому интерфейсы
 * USB, SDIO и генератор случайных чисел на этой частоте недоступны. Для
 * данной работы они не нужны.
 */
static void SystemClock_Config(void)
{
    RCC_OscInitTypeDef osc = { 0 };
    RCC_ClkInitTypeDef clk = { 0 };

    __HAL_RCC_PWR_CLK_ENABLE();
    __HAL_PWR_VOLTAGESCALING_CONFIG(PWR_REGULATOR_VOLTAGE_SCALE1);

    osc.OscillatorType = RCC_OSCILLATORTYPE_HSE;
    osc.HSEState       = RCC_HSE_ON;            /* кварц, не внешний генератор */
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

    clk.ClockType      = RCC_CLOCKTYPE_HCLK   | RCC_CLOCKTYPE_SYSCLK |
                         RCC_CLOCKTYPE_PCLK1  | RCC_CLOCKTYPE_PCLK2;
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
    HAL_Init();
    SystemClock_Config();

    app_main();

    return 0;
}
