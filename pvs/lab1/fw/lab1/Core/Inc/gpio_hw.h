#ifndef GPIO_HW_H
#define GPIO_HW_H

#include <stdint.h>
#include "gpio_regs.h"

/*
 * Единый интерфейс доступа к выводам, которым пользуются драйверы led и button.
 *
 * Существуют две взаимозаменяемые реализации:
 *   gpio_hw_hal.c - поверх стандартного драйвера HAL (конфигурация Debug_HAL);
 *   gpio_hw_my.c  - поверх собственного драйвера mygpio (конфигурация Debug_MyGPIO).
 * Выбор делается на этапе сборки, ни один модуль выше этого заголовка при
 * замене реализации не меняется.
 */

typedef GPIO_TypeDef *gpio_hw_port_t;

typedef enum {
    GPIO_HW_IN = 0,
    GPIO_HW_OUT_PP,
    GPIO_HW_OUT_OD,
    GPIO_HW_ANALOG
} gpio_hw_mode_t;

typedef enum {
    GPIO_HW_NOPULL = 0,
    GPIO_HW_PULLUP,
    GPIO_HW_PULLDOWN
} gpio_hw_pull_t;

/* Не менять текущий уровень на выводе при реинициализации */
#define GPIO_HW_LEVEL_KEEP  0xFFu

void gpio_hw_port_enable(gpio_hw_port_t port);

/*
 * init_level задаётся ДО включения выходного буфера, поэтому переход
 * вывода в режим выхода происходит сразу в нужное состояние.
 */
void gpio_hw_config_mask(gpio_hw_port_t port, uint16_t mask,
                         gpio_hw_mode_t mode, gpio_hw_pull_t pull,
                         uint8_t init_level);

uint16_t gpio_hw_read_mask    (gpio_hw_port_t port, uint16_t mask);
uint16_t gpio_hw_read_out_mask(gpio_hw_port_t port, uint16_t mask);

/* Все выводы маски принимают новые уровни за одну запись */
void     gpio_hw_write_mask   (gpio_hw_port_t port, uint16_t mask, uint16_t values);

#endif /* GPIO_HW_H */
