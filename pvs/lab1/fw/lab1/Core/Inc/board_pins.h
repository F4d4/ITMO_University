#ifndef BOARD_PINS_H
#define BOARD_PINS_H

#include "gpio_regs.h"

/*
 * Карта выводов стенда SDK-1.1M (процессорный модуль STM32F427VIT6).
 *
 * ЭТО ЕДИНСТВЕННЫЙ ФАЙЛ, В КОТОРОМ УПОМИНАЮТСЯ КОНКРЕТНЫЕ НОМЕРА ВЫВОДОВ.
 * Ни драйверы, ни тем более прикладная логика их не знают.
 *
 * Согласно учебному пособию (раздел 1.2) и принципиальной схеме стенда:
 *   кнопка боковой панели          - PC15;
 *   зелёный светодиод              - PD13;
 *   двухцветный красный/жёлтый     - PD14 и PD15.
 */

/* --- Кнопка --- */
#define BOARD_BTN_PORT          GPIOC
#define BOARD_BTN_PIN           15u
#define BOARD_BTN_MASK          ((uint16_t)(1u << BOARD_BTN_PIN))

/*
 * Цепь кнопки на схеме обозначена nBTN: замкнутая кнопка тянет линию к нулю,
 * поэтому активный уровень низкий, а вывод настраивается с подтяжкой вверх.
 * Аппаратной защиты от дребезга в стенде нет - она реализована программно
 * в драйвере button.
 */
#define BOARD_BTN_ACTIVE_LEVEL  0u

/* --- Светодиоды --- */
#define BOARD_LED_PORT          GPIOD
#define BOARD_LED_GREEN_PIN     13u
#define BOARD_LED_BI_A_PIN      14u
#define BOARD_LED_BI_B_PIN      15u

#define BOARD_LED_GREEN_MASK    ((uint16_t)(1u << BOARD_LED_GREEN_PIN))
#define BOARD_LED_BI_A_MASK     ((uint16_t)(1u << BOARD_LED_BI_A_PIN))
#define BOARD_LED_BI_B_MASK     ((uint16_t)(1u << BOARD_LED_BI_B_PIN))
#define BOARD_LED_BI_MASK       (BOARD_LED_BI_A_MASK | BOARD_LED_BI_B_MASK)
#define BOARD_LED_ALL_MASK      (BOARD_LED_GREEN_MASK | BOARD_LED_BI_MASK)

/*
 * Полярность светодиодов. Значения подтверждаются пробой полярности
 * (см. fw/probe) и при необходимости правятся здесь и в таблице
 * b_led_patterns[] в board_leds.c - больше нигде.
 */
#define BOARD_LED_GREEN_ACTIVE_LEVEL  1u

/*
 * Двухцветный светодиод включён встречно-параллельно: два вывода, один
 * двухвыводной прибор. Цвет определяется направлением тока, поэтому
 * зажечь оба цвета одновременно физически невозможно, а смена цвета
 * обязана выполняться одной записью в порт.
 */
#define BOARD_LED_BI_ANTIPARALLEL  1

#endif /* BOARD_PINS_H */
