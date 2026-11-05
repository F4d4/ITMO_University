#include "uart_drv.h"
#include "uart_hal.h"
#include "app_config.h"
#include "board.h"
#include "board_pins.h"
#include "main.h"

/*
 * Общая часть драйверов UART на основе стандартного драйвера HAL:
 * контекст драйвера, однократная инициализация блока и вектор прерывания.
 */

UART_HandleTypeDef uart_hal_handle;

/* Вызывается из HAL_UART_Init(): низкоуровневая часть - тактирование и выводы */
void HAL_UART_MspInit(UART_HandleTypeDef *huart)
{
    if (huart != &uart_hal_handle) {
        return;
    }

    board_uart_hw_init();

    /*
     * Приоритет назначается заранее, но само прерывание в NVIC разрешает
     * только драйвер по прерываниям при запуске: в режиме опроса оно должно
     * оставаться запрещённым.
     */
    HAL_NVIC_SetPriority(board_uart_irqn(), SERIAL_IRQ_PRIORITY, 0u);
    HAL_NVIC_DisableIRQ(board_uart_irqn());
}

void uart_port_init(uint32_t baud)
{
    uart_hal_handle.Instance          = board_uart_regs();
    uart_hal_handle.Init.BaudRate     = baud;
    uart_hal_handle.Init.WordLength   = UART_WORDLENGTH_8B;
    uart_hal_handle.Init.StopBits     = UART_STOPBITS_1;
    uart_hal_handle.Init.Parity       = UART_PARITY_NONE;
    uart_hal_handle.Init.Mode         = UART_MODE_TX_RX;
    uart_hal_handle.Init.HwFlowCtl    = UART_HWCONTROL_NONE;
    uart_hal_handle.Init.OverSampling = UART_OVERSAMPLING_16;

    if (HAL_UART_Init(&uart_hal_handle) != HAL_OK) {
        Error_Handler();
    }
}

/*
 * Обработчик привязан к вектору блока USART стенда и передаёт управление
 * библиотечному обработчику, который по флагам определяет причину прерывания
 * и вызывает HAL_UART_RxCpltCallback / TxCpltCallback / ErrorCallback.
 */
void BOARD_UART_IRQHandler(void)
{
    HAL_UART_IRQHandler(&uart_hal_handle);
}
