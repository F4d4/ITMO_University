#include "serial.h"
#include "app_config.h"
#include "fifo.h"
#include "uart_drv.h"

static uint8_t b_rx_mem[FIFO_STORAGE_SIZE(SERIAL_RX_BUF_SIZE)];
static uint8_t b_tx_mem[FIFO_STORAGE_SIZE(SERIAL_TX_BUF_SIZE)];

/*
 * Читатель rx - главный цикл (serial_read), писатель - драйвер.
 * Писатель tx - главный цикл (serial_write), читатель - драйвер.
 * Ровно по одному с каждой стороны, как того требует fifo.
 */
static fifo_t b_rx;
static fifo_t b_tx;

static const uart_drv_t *b_drv;
static serial_mode_t     b_mode;

static const uart_drv_t *serial_driver(serial_mode_t mode)
{
    return (mode == SERIAL_MODE_IRQ) ? &uart_drv_irq : &uart_drv_poll;
}

void serial_init(serial_mode_t mode)
{
    fifo_init(&b_rx, b_rx_mem, (uint16_t)sizeof(b_rx_mem));
    fifo_init(&b_tx, b_tx_mem, (uint16_t)sizeof(b_tx_mem));

    uart_port_init(SERIAL_BAUD);

    b_mode = mode;
    b_drv  = serial_driver(mode);
    b_drv->start(&b_rx, &b_tx);
}

void serial_set_mode(serial_mode_t mode)
{
    if (mode == b_mode) {
        return;
    }

    /*
     * Старый драйвер полностью отпускает блок USART до того, как его возьмёт
     * новый: два драйвера никогда не обслуживают очереди одновременно.
     */
    b_drv->stop();
    b_mode = mode;
    b_drv  = serial_driver(mode);
    b_drv->start(&b_rx, &b_tx);
}

serial_mode_t serial_get_mode(void)
{
    return b_mode;
}

void serial_task(void)
{
    b_drv->poll();
}

bool serial_read(uint8_t *byte)
{
    return fifo_get(&b_rx, byte);
}

bool serial_write(const void *data, uint16_t len)
{
    if (!fifo_write(&b_tx, (const uint8_t *)data, len)) {
        return false;
    }
    b_drv->kick();
    return true;
}
