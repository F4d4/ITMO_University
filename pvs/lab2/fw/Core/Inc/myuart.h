#ifndef MYUART_H
#define MYUART_H

#include <stdint.h>
#include <stdbool.h>

#include "stm32f4xx.h"
#include "fifo.h"

/*
 * Собственный драйвер USART для STM32F4 (часть 2 работы, способ 1).
 *
 * Работает напрямую с регистрами блока через структуру USART_TypeDef и
 * определения битов из CMSIS - это разрешено заданием. Ни функции, ни
 * структуры HAL/LL для UART/USART не используются: в конфигурации сборки
 * "myuart" модули HAL UART и USART исключены целиком, и случайное обращение
 * к ним не скомпилируется.
 *
 * Формат кадра фиксирован - 8 бит данных, без контроля чётности, 1 стоп-бит:
 * другие форматы этой программе не нужны.
 */

typedef enum {
    MYUART_MODE_OFF = 0,    /* блок включён, но очереди не обслуживаются */
    MYUART_MODE_POLLING,    /* обмен выполняет myuart_poll(), прерывание запрещено */
    MYUART_MODE_IRQ         /* обмен выполняет обработчик прерывания */
} myuart_mode_t;

typedef struct {
    USART_TypeDef *regs;
    IRQn_Type      irqn;
    uint32_t       irq_priority;   /* приоритет в NVIC: меньше число - выше приоритет */
    uint32_t       clock_hz;       /* частота тактирования блока (PCLK шины) */
    uint32_t       baud;
} myuart_config_t;

/* Накопленные события; меняются только драйвером */
typedef struct {
    uint32_t overrun;       /* ORE: байт пришёл, пока предыдущий не был прочитан */
    uint32_t framing;       /* FE: не найден стоп-бит */
    uint32_t noise;         /* NF: шум на линии при приёме */
    uint32_t rx_dropped;    /* принятые байты, для которых не нашлось места в очереди */
} myuart_stats_t;

typedef struct {
    bool           rx_ready;     /* RXNE: в регистре данных есть принятый байт */
    bool           tx_ready;     /* TXE: регистр данных передатчика свободен */
    bool           tx_complete;  /* TC: передатчик закончил передачу, линия свободна */
    myuart_mode_t  mode;
    myuart_stats_t stats;
} myuart_status_t;

typedef struct {
    USART_TypeDef          *regs;
    IRQn_Type               irqn;
    volatile myuart_mode_t  mode;
    fifo_t                 *rx;
    fifo_t                 *tx;
    volatile myuart_stats_t stats;
} myuart_t;

/*
 * Однократная настройка: формат кадра, скорость, включение блока. Режим
 * после инициализации - MYUART_MODE_OFF. Тактирование блока и выводы TX/RX
 * должен заранее подготовить вызывающий.
 */
void myuart_init(myuart_t *u, const myuart_config_t *cfg);

/*
 * Запуск обмена в режиме прерываний или опроса. Принятые байты кладутся в
 * rx, передаваемые берутся из tx. Очереди принадлежат вызывающему и при
 * смене режима сохраняют своё содержимое.
 */
void myuart_start(myuart_t *u, myuart_mode_t mode, fifo_t *rx, fifo_t *tx);

/* Прекратить обслуживание очередей; ещё не переданное остаётся в очереди tx */
void myuart_stop(myuart_t *u);

/* В режиме прерываний: в очереди tx появились данные - начать передачу */
void myuart_kick(myuart_t *u);

/* В режиме опроса: один шаг обмена, без ожидания */
void myuart_poll(myuart_t *u);

/* Вызывать из обработчика прерывания блока USART */
void myuart_irq_handler(myuart_t *u);

/* --- Неблокирующие примитивы обмена одним байтом --- */

/* Забрать принятый байт; false - принятых данных нет */
bool myuart_read_byte(myuart_t *u, uint8_t *byte);

/* Отдать байт передатчику; false - регистр данных занят, байт не принят */
bool myuart_write_byte(myuart_t *u, uint8_t byte);

/* --- Проверка состояния --- */
void myuart_get_status(const myuart_t *u, myuart_status_t *out);

#endif /* MYUART_H */
