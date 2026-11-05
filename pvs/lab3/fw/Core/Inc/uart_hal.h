#ifndef UART_HAL_H
#define UART_HAL_H

#include "stm32f4xx_hal.h"

/*
 * Внутренний заголовок драйверов UART поверх HAL (конфигурация "hal").
 * Оба драйвера - по прерываниям и по опросу - работают с одним и тем же
 * контекстом стандартного драйвера.
 */
extern UART_HandleTypeDef uart_hal_handle;

#endif /* UART_HAL_H */
