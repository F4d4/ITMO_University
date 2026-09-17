#ifndef BOARD_H
#define BOARD_H

#include <stdbool.h>
#include "led.h"

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

#endif /* BOARD_H */
