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

/* --- Шина I2C --- */
I2C_TypeDef   *board_i2c_regs(void);
IRQn_Type      board_i2c_ev_irqn(void);
IRQn_Type      board_i2c_er_irqn(void);

/* Включить тактирование блока I2C и передать ему выводы SCL/SDA */
void           board_i2c_hw_init(void);

/*
 * Освободить шину, которую удерживает ведомый: если сброс микроконтроллера
 * пришёлся на середину чтения, ведомый продолжает выдавать байт и держит SDA
 * в нуле. До девяти тактов на SCL дают ему дочитать байт, затем формируется
 * STOP. Выводы остаются выходами GPIO; вызывать до board_i2c_hw_init().
 */
void           board_i2c_bus_recover(void);

/* --- Таймер расписания I2C --- */
TIM_TypeDef   *board_sched_tim_regs(void);
IRQn_Type      board_sched_tim_irqn(void);
void           board_sched_tim_clk_enable(void);

/* Частота, которой тактируется счётчик таймера расписания, Гц */
uint32_t       board_sched_tim_clock_hz(void);

/* Активное ожидание по счётчику тактов ядра; только для коротких пауз */
void           board_delay_us(uint32_t us);

/* Счётчик тактов ядра (DWT), для измерения длительности обменов */
uint32_t       board_cycles(void);
uint32_t       board_cycles_per_us(void);

#endif /* BOARD_H */
