#ifndef GPIO_REGS_H
#define GPIO_REGS_H

/*
 * Определения GPIO_TypeDef и базовых адресов.
 * На стенде берутся из CMSIS (заданием это прямо разрешено),
 * в симуляторе - из совместимой по раскладке заглушки.
 */
#ifdef SIM_BUILD
#  include "cmsis_shim.h"
#else
#  include "stm32f4xx.h"
#endif

#endif /* GPIO_REGS_H */
