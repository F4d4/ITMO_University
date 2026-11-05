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

/*
 * --- Шина I2C стенда (пособие, раздел 1.6): I2C1, SCL - PB8, SDA - PB9 ---
 * Подтяжки 10 кОм к +3,3 В стоят на плате (DR6.2, DR6.3), внутренние не нужны.
 */
#define BOARD_I2C               I2C1
#define BOARD_I2C_EV_IRQn       I2C1_EV_IRQn
#define BOARD_I2C_ER_IRQn       I2C1_ER_IRQn
#define BOARD_I2C_EV_IRQHandler I2C1_EV_IRQHandler
#define BOARD_I2C_ER_IRQHandler I2C1_ER_IRQHandler
#define BOARD_I2C_PORT          GPIOB
#define BOARD_I2C_SCL_PIN       8u
#define BOARD_I2C_SDA_PIN       9u
#define BOARD_I2C_SCL_MASK      ((uint16_t)(1u << BOARD_I2C_SCL_PIN))
#define BOARD_I2C_SDA_MASK      ((uint16_t)(1u << BOARD_I2C_SDA_PIN))
#define BOARD_I2C_AF            4u      /* AF4 = I2C1..3 */
#define BOARD_I2C_CLK_ENABLE()  __HAL_RCC_I2C1_CLK_ENABLE()

/*
 * Адреса устройств на шине - 8-битные, как их принимает HAL (7-битный адрес,
 * сдвинутый влево). Источник - принципиальная схема, лист 4.
 */
#define BOARD_I2C_ADDR_OLED     0x78u   /* WEO012864DL (SSD1306), SA0 = 0 */
#define BOARD_I2C_ADDR_IOEXP    0xE0u   /* PCA9538 DD10: A1 = A0 = 0; P0 - RST# дисплея */
#define BOARD_I2C_ADDR_KEYPAD   0xE2u   /* PCA9538 DD12: A1 = 0, A0 = 1; клавиатура */
#define BOARD_I2C_ADDR_ACCEL    0xD4u   /* LSM9DS1 акселерометр/гироскоп, SDO_A/G = GND */

/* Вывод расширителя 0xE0, к которому подключён RST# дисплея */
#define BOARD_IOEXP_OLED_RST_MASK  0x01u

/*
 * Клавиатура (расширитель 0xE2): строки на P0..P3, столбцы на P4..P6.
 * На лицевой панели строка P0 верхняя, столбец P4 левый (рис. 37 пособия).
 */
#define BOARD_KEYPAD_ROWS       4u
#define BOARD_KEYPAD_COLS       3u
#define BOARD_KEYPAD_ROW_SHIFT  0u
#define BOARD_KEYPAD_COL_SHIFT  4u

/*
 * Положение акселерометра относительно экрана: какая ось датчика (0 - X,
 * 1 - Y, 2 - Z) и с каким знаком направлена вправо и вниз по экрану.
 *
 * Измерено на стенде (в покое датчик показывает +1g по оси, смотрящей вверх):
 *   стенд стоит клавиатурой вниз            - Y = +1g: ось +Y направлена вверх по экрану;
 *   стенд стоит правой стороной (Ethernet) вниз - X = -1g: ось +X направлена вправо;
 *   стенд лежит дисплеем вверх              - Z = -1g (ось Z для указателя не нужна).
 * Направление стрелки при наклонах подтверждено визуально.
 */
#define BOARD_ACCEL_RIGHT_AXIS  0
#define BOARD_ACCEL_RIGHT_SIGN  (+1)
#define BOARD_ACCEL_DOWN_AXIS   1
#define BOARD_ACCEL_DOWN_SIGN   (-1)

/* --- Таймер, задающий расписание обмена по I2C --- */
#define BOARD_SCHED_TIM             TIM6
#define BOARD_SCHED_TIM_IRQn        TIM6_DAC_IRQn
#define BOARD_SCHED_TIM_IRQHandler  TIM6_DAC_IRQHandler
#define BOARD_SCHED_TIM_CLK_ENABLE() __HAL_RCC_TIM6_CLK_ENABLE()

#endif /* BOARD_PINS_H */
