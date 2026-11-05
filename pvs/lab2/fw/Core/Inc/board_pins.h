#ifndef BOARD_PINS_H
#define BOARD_PINS_H

#include "stm32f4xx.h"

/*
 * Карта выводов и периферии стенда SDK-1.1M (процессорный модуль STM32F427VIT6).
 *
 * ЭТО ЕДИНСТВЕННЫЙ ФАЙЛ, В КОТОРОМ УПОМИНАЮТСЯ КОНКРЕТНЫЕ НОМЕРА ВЫВОДОВ И
 * БЛОКИ ПЕРИФЕРИИ. Прикладная логика их не знает.
 *
 * Согласно учебному пособию (разделы 1.2, 1.4) и принципиальной схеме стенда:
 *   кнопка боковой панели          - PC15;
 *   зелёный светодиод              - PD13;
 *   двухцветный красный/жёлтый     - PD14 и PD15;
 *   отладочный UART (виртуальный COM-порт встроенного отладчика) -
 *   USART6: TX - PC6, RX - PC7.
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

/* Полярность подтверждена в лабораторной работе 1 */
#define BOARD_LED_GREEN_ACTIVE_LEVEL  1u

/* --- Отладочный UART --- */
#define BOARD_UART              USART6
#define BOARD_UART_IRQn         USART6_IRQn
#define BOARD_UART_IRQHandler   USART6_IRQHandler
#define BOARD_UART_PORT         GPIOC
#define BOARD_UART_TX_PIN       6u
#define BOARD_UART_RX_PIN       7u
#define BOARD_UART_TX_MASK      ((uint16_t)(1u << BOARD_UART_TX_PIN))
#define BOARD_UART_RX_MASK      ((uint16_t)(1u << BOARD_UART_RX_PIN))
#define BOARD_UART_AF           8u      /* AF8 = USART4..6 по таблице datasheet DS9405 */
#define BOARD_UART_CLK_ENABLE() __HAL_RCC_USART6_CLK_ENABLE()
#define BOARD_UART_CLK_HZ()     HAL_RCC_GetPCLK2Freq()   /* USART1/USART6 сидят на APB2 */

#endif /* BOARD_PINS_H */
