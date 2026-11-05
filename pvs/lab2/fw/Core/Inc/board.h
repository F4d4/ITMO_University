#ifndef BOARD_H
#define BOARD_H

#include <stdbool.h>
#include <stdint.h>

#include "led.h"
#include "stm32f4xx.h"

/*
 * Уровень карты платы. Только здесь знание о том, какими выводами и какими
 * уровнями управляются конкретные приборы стенда, превращается в вызовы
 * абстрактного интерфейса gpio_hw.
 */

/* Тактирование портов и настройка режимов работы выводов */
void board_init(void);

/* Применить цвет канала одной записью в порт */
void board_led_apply(led_ch_t ch, led_color_t color);

/* Текущий уровень на выводе кнопки с учётом её активного уровня */
bool board_button_is_closed(void);

/*
 * Отладочный UART. Драйверы UART получают от платы блок регистров, номер
 * прерывания и частоту тактирования, не зная, какие это конкретно USART и
 * выводы.
 */
USART_TypeDef *board_uart_regs(void);
IRQn_Type      board_uart_irqn(void);

/* Включить тактирование блока USART и передать ему выводы TX/RX */
void           board_uart_hw_init(void);

/* Частота шины, от которой тактируется блок USART, Гц */
uint32_t       board_uart_clock_hz(void);

#endif /* BOARD_H */
