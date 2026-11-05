#ifndef SYS_TICK_H
#define SYS_TICK_H

#include <stdint.h>

/*
 * Абстракция миллисекундного счётчика. На стенде реализуется через
 * HAL_GetTick() (SysTick), в симуляторе - через модельное время.
 * Благодаря этому вся логика выше по слоям компилируется без изменений
 * и на цель, и на хост.
 */
uint32_t sys_tick_ms(void);

#endif /* SYS_TICK_H */
