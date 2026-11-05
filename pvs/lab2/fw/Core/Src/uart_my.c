#include "uart_drv.h"
#include "myuart.h"
#include "app_config.h"
#include "board.h"
#include "board_pins.h"

/*
 * Драйверы UART по прерываниям и по опросу поверх собственного регистрового
 * драйвера myuart (конфигурация сборки "myuart"). Поведение для программы то
 * же, что у конфигурации "hal": меняется только то, что лежит под интерфейсом
 * uart_drv_t.
 */

static myuart_t b_uart;

void uart_port_init(uint32_t baud)
{
    board_uart_hw_init();

    const myuart_config_t cfg = {
        .regs         = board_uart_regs(),
        .irqn         = board_uart_irqn(),
        .irq_priority = SERIAL_IRQ_PRIORITY,
        .clock_hz     = board_uart_clock_hz(),
        .baud         = baud
    };
    myuart_init(&b_uart, &cfg);
}

void BOARD_UART_IRQHandler(void)
{
    myuart_irq_handler(&b_uart);
}

static void uart_my_irq_start(fifo_t *rx, fifo_t *tx)
{
    myuart_start(&b_uart, MYUART_MODE_IRQ, rx, tx);
}

static void uart_my_poll_start(fifo_t *rx, fifo_t *tx)
{
    myuart_start(&b_uart, MYUART_MODE_POLLING, rx, tx);
}

static void uart_my_stop(void)
{
    myuart_stop(&b_uart);
}

static void uart_my_kick(void)
{
    myuart_kick(&b_uart);
}

static void uart_my_poll(void)
{
    myuart_poll(&b_uart);
}

const uart_drv_t uart_drv_irq = {
    .start = uart_my_irq_start,
    .stop  = uart_my_stop,
    .kick  = uart_my_kick,
    .poll  = uart_my_poll
};

const uart_drv_t uart_drv_poll = {
    .start = uart_my_poll_start,
    .stop  = uart_my_stop,
    .kick  = uart_my_kick,
    .poll  = uart_my_poll
};
