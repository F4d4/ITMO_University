#include "uart_drv.h"
#include "uart_hal.h"
#include "board.h"

/*
 * Драйвер UART по опросу на основе стандартного драйвера HAL.
 *
 * Прерывание блока USART запрещено. Обмен выполняет poll(), которую главный
 * цикл вызывает на каждом проходе: за вызов принимается не более одного байта
 * и передаётся не более одного. Ни одна функция драйвера не ждёт ни приёма,
 * ни окончания передачи.
 *
 * Неблокирующий режим получается за счёт нулевого тайм-аута в функциях HAL:
 *
 *  - HAL_UART_Receive(..., 1, 0): если в регистре данных есть принятый байт
 *    (RXNE = 1), он читается и возвращается HAL_OK; иначе функция сразу
 *    возвращает HAL_TIMEOUT - "данных нет";
 *
 *  - HAL_UART_Transmit(..., 1, 0): если регистр данных передатчика свободен
 *    (TXE = 1), байт записывается в него, после чего функция возвращает
 *    HAL_TIMEOUT уже на ожидании окончания передачи (TC). Это ожидание нам и
 *    не нужно: дальше байт передаёт аппаратура. Если регистр занят, HAL сразу
 *    возвращает HAL_TIMEOUT, ничего не записав. Различить два случая
 *    позволяет счётчик TxXferCount: HAL уменьшает его до нуля только после
 *    записи байта в регистр данных.
 *
 * На ветке тайм-аута HAL дополнительно сбрасывает разрешения прерываний
 * блока USART - в режиме опроса они и так выключены.
 */

static fifo_t *b_rx;
static fifo_t *b_tx;

static void uart_poll_start(fifo_t *rx, fifo_t *tx)
{
    b_rx = rx;
    b_tx = tx;

    /* Режим без прерываний: прерывание блока USART запрещено в NVIC */
    HAL_NVIC_DisableIRQ(board_uart_irqn());
    HAL_NVIC_ClearPendingIRQ(board_uart_irqn());
}

static void uart_poll_stop(void)
{
    /* Незавершённых операций нет: каждый вызов poll() заканчивает свою работу */
}

static void uart_poll_kick(void)
{
    /* Очередь tx разбирает poll() */
}

static void uart_poll_poll(void)
{
    uint8_t byte;

    if (HAL_UART_Receive(&uart_hal_handle, &byte, 1u, 0u) == HAL_OK) {
        /* Очередь полна - байт теряется: принятые раньше данные важнее */
        (void)fifo_put(b_rx, byte);
    }

    const uint8_t *data;

    if (fifo_peek_linear(b_tx, &data) != 0u) {
        const HAL_StatusTypeDef st = HAL_UART_Transmit(&uart_hal_handle, data, 1u, 0u);

        if ((st != HAL_BUSY) && (uart_hal_handle.TxXferCount == 0u)) {
            fifo_drop(b_tx, 1u);
        }
    }
}

const uart_drv_t uart_drv_poll = {
    .start = uart_poll_start,
    .stop  = uart_poll_stop,
    .kick  = uart_poll_kick,
    .poll  = uart_poll_poll
};
