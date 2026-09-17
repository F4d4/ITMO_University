#ifndef NAIVE_GPIO_H
#define NAIVE_GPIO_H

#include <stdint.h>
#include "gpio_regs.h"

/*
 * Намеренно неаккуратная инициализация вывода: режим включается первым, а
 * выходная защёлка загружается уже после. Используется только в симуляторе
 * как образец для сравнения - в прошивку не входит.
 */
void naive_gpio_init_output(GPIO_TypeDef *port, uint8_t pin, uint8_t level);

#endif /* NAIVE_GPIO_H */
