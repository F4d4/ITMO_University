#include "uart_drv.h"
#include "uart_hal.h"
#include "board.h"
#include "main.h"

/*
 * Драйвер UART по прерываниям на основе стандартного драйвера HAL.
 *
 * Приём. HAL_UART_Receive_IT() всё время держит заказ на один байт. Принятый
 * байт обработчик прерывания передаёт в HAL_UART_RxCpltCallback(), тот кладёт
 * его в очередь rx и сразу делает новый заказ. Главный цикл забирает байты из
 * очереди когда ему удобно и ничего не ждёт.
 *
 * Передача. Главный цикл только дописывает данные в очередь tx и вызывает
 * kick(). Если передача не идёт, kick() отдаёт HAL_UART_Transmit_IT() самый
 * длинный непрерывный участок очереди - прямо из её памяти, без копирования.
 * Байты освобождаются в HAL_UART_TxCpltCallback(), когда они действительно
 * ушли в линию; там же запускается передача следующего участка. Передача
 * останавливается сама, когда очередь опустела.
 *
 * Взаимное исключение. Функции HAL для одного контекста нельзя вызывать
 * одновременно из главного цикла и из прерывания (они захватывают
 * huart->Lock и меняют состояние контекста). Поэтому всё, что главный цикл
 * делает с контекстом HAL, выполняется в критической секции, в которой
 * запрещено только прерывание блока USART: остальные прерывания (системный
 * таймер) продолжают работать.
 */

static fifo_t  *b_rx;
static fifo_t  *b_tx;
static uint8_t  b_rx_byte;
static uint16_t b_tx_len;   /* длина участка, отданного HAL; 0 - передача не идёт */

static void uart_irq_lock(void)
{
    HAL_NVIC_DisableIRQ(board_uart_irqn());
}

static void uart_irq_unlock(void)
{
    HAL_NVIC_EnableIRQ(board_uart_irqn());
}

static void uart_irq_rx_arm(void)
{
    if (HAL_UART_Receive_IT(&uart_hal_handle, &b_rx_byte, 1u) != HAL_OK) {
        Error_Handler();
    }
}

/* Вызывается из прерывания или из главного цикла при запрещённом прерывании USART */
static void uart_irq_tx_next(void)
{
    const uint8_t *data;
    const uint16_t len = fifo_peek_linear(b_tx, &data);

    b_tx_len = len;
    if (len == 0u) {
        return;
    }
    if (HAL_UART_Transmit_IT(&uart_hal_handle, data, len) != HAL_OK) {
        Error_Handler();
    }
}

void HAL_UART_RxCpltCallback(UART_HandleTypeDef *huart)
{
    if (huart != &uart_hal_handle) {
        return;
    }
    /* Очередь полна - байт теряется: принятые раньше данные важнее */
    (void)fifo_put(b_rx, b_rx_byte);
    uart_irq_rx_arm();
}

void HAL_UART_TxCpltCallback(UART_HandleTypeDef *huart)
{
    if (huart != &uart_hal_handle) {
        return;
    }
    fifo_drop(b_tx, b_tx_len);
    uart_irq_tx_next();
}

void HAL_UART_ErrorCallback(UART_HandleTypeDef *huart)
{
    if (huart != &uart_hal_handle) {
        return;
    }
    /*
     * Переполнение приёмника (ORE) HAL считает блокирующей ошибкой: приём
     * прекращается, RxState возвращается в READY. Ошибки кадра и шума
     * приём не прерывают. Возобновляем приём, только если он остановлен.
     */
    if (huart->RxState == HAL_UART_STATE_READY) {
        uart_irq_rx_arm();
    }
}

static void uart_irq_start(fifo_t *rx, fifo_t *tx)
{
    b_rx     = rx;
    b_tx     = tx;
    b_tx_len = 0u;

    /* Прерывание ещё запрещено в NVIC: заказы делаются без гонок */
    HAL_NVIC_ClearPendingIRQ(board_uart_irqn());
    uart_irq_rx_arm();
    uart_irq_tx_next();     /* досылаем то, что осталось от другого режима */
    uart_irq_unlock();
}

static void uart_irq_stop(void)
{
    uart_irq_lock();

    if (b_tx_len != 0u) {
        /*
         * Байты, которые HAL уже записал в регистр данных, уйдут в линию и
         * без участия драйвера - их из очереди убираем. Остальные остаются
         * в очереди и будут переданы следующим драйвером.
         */
        const uint16_t written = (uint16_t)(uart_hal_handle.TxXferSize - uart_hal_handle.TxXferCount);

        fifo_drop(b_tx, written);
        b_tx_len = 0u;
    }

    if (HAL_UART_Abort(&uart_hal_handle) != HAL_OK) {
        Error_Handler();
    }
    HAL_NVIC_ClearPendingIRQ(board_uart_irqn());
}

static void uart_irq_kick(void)
{
    uart_irq_lock();
    if (b_tx_len == 0u) {
        uart_irq_tx_next();
    }
    uart_irq_unlock();
}

static void uart_irq_poll(void)
{
    /* Обмен идёт в прерываниях, главному циклу делать нечего */
}

const uart_drv_t uart_drv_irq = {
    .start = uart_irq_start,
    .stop  = uart_irq_stop,
    .kick  = uart_irq_kick,
    .poll  = uart_irq_poll
};
