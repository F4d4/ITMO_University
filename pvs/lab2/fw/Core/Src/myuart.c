#include <stddef.h>

#include "myuart.h"

/*
 * Регистры блока USART (RM0090, раздел "Universal synchronous asynchronous
 * receiver transmitter"), которые использует драйвер:
 *
 *   SR  - состояние: RXNE (принят байт), TXE (регистр данных передатчика
 *         свободен), TC (передача завершена), ORE/FE/NF/PE (ошибки приёма);
 *   DR  - регистр данных: запись отдаёт байт передатчику, чтение забирает
 *         принятый байт;
 *   BRR - делитель частоты, задающий скорость обмена;
 *   CR1 - включение блока (UE), передатчика (TE), приёмника (RE), формат
 *         кадра (M, PCE) и разрешения прерываний (RXNEIE, TXEIE, ...);
 *   CR2 - число стоп-битов и синхронный режим;
 *   CR3 - управление потоком, DMA, специальные режимы.
 */

#define MYUART_CR1_IRQ_BITS  (USART_CR1_RXNEIE | USART_CR1_TXEIE | USART_CR1_TCIE | \
                              USART_CR1_PEIE   | USART_CR1_IDLEIE)

static uint32_t myuart_brr(uint32_t clock_hz, uint32_t baud)
{
    /*
     * При передискретизации 16 (OVER8 = 0) USARTDIV = f / (16 * baud), в BRR
     * записываются его целая часть (биты 15:4) и дробная часть в шестнадцатых
     * долях (биты 3:0). Вместе это число 16 * USARTDIV = f / baud, округлённое
     * до целого; перенос из дробной части в целую получается сам собой.
     * Для 90 МГц и 115200 бит/с: 781 = 0x30D (48 + 13/16), ошибка скорости 0,03 %.
     */
    return (clock_hz + baud / 2u) / baud;
}

/* Снять все разрешения прерываний блока и само прерывание в NVIC */
static void myuart_irq_off(myuart_t *u)
{
    NVIC_DisableIRQ(u->irqn);
    u->regs->CR1 &= ~MYUART_CR1_IRQ_BITS;
    NVIC_ClearPendingIRQ(u->irqn);
}

static void myuart_count_errors(myuart_t *u, uint32_t sr)
{
    if ((sr & USART_SR_ORE) != 0u) { u->stats.overrun++; }
    if ((sr & USART_SR_FE)  != 0u) { u->stats.framing++; }
    if ((sr & USART_SR_NE)  != 0u) { u->stats.noise++;   }
}

/*
 * Один шаг обмена: забрать принятый байт и отдать передатчику следующий.
 * Общий для обработчика прерывания и для режима опроса.
 */
static void myuart_service(myuart_t *u)
{
    uint8_t byte;

    if (myuart_read_byte(u, &byte) && !fifo_put(u->rx, byte)) {
        /* Очередь полна - байт теряется: принятые раньше данные важнее */
        u->stats.rx_dropped++;
    }

    const uint8_t *data;

    if ((fifo_peek_linear(u->tx, &data) != 0u) && myuart_write_byte(u, data[0])) {
        fifo_drop(u->tx, 1u);
    }
}

void myuart_init(myuart_t *u, const myuart_config_t *cfg)
{
    USART_TypeDef *r = cfg->regs;

    u->regs  = r;
    u->irqn  = cfg->irqn;
    u->mode  = MYUART_MODE_OFF;
    u->rx    = NULL;
    u->tx    = NULL;
    u->stats.overrun    = 0u;
    u->stats.framing    = 0u;
    u->stats.noise      = 0u;
    u->stats.rx_dropped = 0u;

    NVIC_DisableIRQ(cfg->irqn);
    /*
     * HAL_Init() выбирает группировку приоритетов, при которой все 4 бита
     * приоритета в STM32F4 - вытесняющие, поэтому это то же самое, что
     * HAL_NVIC_SetPriority(irqn, irq_priority, 0).
     */
    NVIC_SetPriority(cfg->irqn, cfg->irq_priority);

    r->CR1 = 0u;    /* UE = 0: блок выключен, формат кадра можно менять */
    r->CR2 = 0u;    /* STOP = 00: 1 стоп-бит; CLKEN = 0: асинхронный режим */
    r->CR3 = 0u;    /* без управления потоком, DMA, IrDA, smartcard, полудуплекса */
    r->BRR = myuart_brr(cfg->clock_hz, cfg->baud);

    /*
     * M = 0 - 8 бит данных, PCE = 0 - без контроля чётности, OVER8 = 0 -
     * передискретизация 16. Передатчик и приёмник включаются вместе с блоком;
     * включённый передатчик сначала выдаёт в линию кадр простоя.
     */
    r->CR1 = USART_CR1_UE | USART_CR1_TE | USART_CR1_RE;
}

void myuart_start(myuart_t *u, myuart_mode_t mode, fifo_t *rx, fifo_t *tx)
{
    myuart_irq_off(u);

    u->rx   = rx;
    u->tx   = tx;
    u->mode = mode;

    if (mode == MYUART_MODE_IRQ) {
        /*
         * TXEIE разрешается сразу: при свободном передатчике прерывание
         * возникнет немедленно и либо передаст остаток очереди tx, либо, если
         * она пуста, снимет TXEIE.
         */
        u->regs->CR1 |= USART_CR1_RXNEIE | USART_CR1_TXEIE;
        NVIC_EnableIRQ(u->irqn);
    }
}

void myuart_stop(myuart_t *u)
{
    /* Байт, уже записанный в DR, аппаратура передаст сама; из очереди он уже убран */
    myuart_irq_off(u);
    u->mode = MYUART_MODE_OFF;
}

void myuart_kick(myuart_t *u)
{
    if (u->mode != MYUART_MODE_IRQ) {
        return;     /* в режиме опроса очередь разбирает myuart_poll() */
    }

    /*
     * Чтение-модификация-запись CR1 выполняется при запрещённом прерывании
     * блока: обработчик сам меняет CR1 (снимает TXEIE), и без запрета его
     * изменение могло бы затереться.
     */
    NVIC_DisableIRQ(u->irqn);
    u->regs->CR1 |= USART_CR1_TXEIE;
    NVIC_EnableIRQ(u->irqn);
}

void myuart_poll(myuart_t *u)
{
    /* В режиме прерываний очереди обслуживает обработчик, второй читатель недопустим */
    if (u->mode == MYUART_MODE_POLLING) {
        myuart_service(u);
    }
}

void myuart_irq_handler(myuart_t *u)
{
    myuart_service(u);

    if (fifo_count(u->tx) == 0u) {
        /* Передавать нечего: иначе прерывание по TXE возникало бы непрерывно */
        u->regs->CR1 &= ~USART_CR1_TXEIE;
    }
}

bool myuart_read_byte(myuart_t *u, uint8_t *byte)
{
    const uint32_t sr = u->regs->SR;

    if ((sr & USART_SR_RXNE) == 0u) {
        return false;
    }

    /*
     * Последовательность "чтение SR, затем чтение DR" сбрасывает RXNE и
     * флаги ошибок ORE, FE, NF, PE. Флаги взяты из того же снимка SR, что и
     * RXNE, поэтому относятся именно к этому байту. Флаг ORE при этом
     * означает, что потерян байт, пришедший вслед за прочитанным.
     */
    *byte = (uint8_t)u->regs->DR;
    myuart_count_errors(u, sr);
    return true;
}

bool myuart_write_byte(myuart_t *u, uint8_t byte)
{
    if ((u->regs->SR & USART_SR_TXE) == 0u) {
        return false;
    }

    /* Запись в DR сбрасывает TXE; байт уходит в сдвиговый регистр передатчика */
    u->regs->DR = byte;
    return true;
}

void myuart_get_status(const myuart_t *u, myuart_status_t *out)
{
    const uint32_t sr = u->regs->SR;

    out->rx_ready    = (sr & USART_SR_RXNE) != 0u;
    out->tx_ready    = (sr & USART_SR_TXE)  != 0u;
    out->tx_complete = (sr & USART_SR_TC)   != 0u;
    out->mode        = u->mode;

    out->stats.overrun    = u->stats.overrun;
    out->stats.framing    = u->stats.framing;
    out->stats.noise      = u->stats.noise;
    out->stats.rx_dropped = u->stats.rx_dropped;
}
