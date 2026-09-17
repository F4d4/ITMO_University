#ifndef GPIO_BUS_H
#define GPIO_BUS_H

#include <stdint.h>
#include "gpio_regs.h"

/*
 * Шина доступа к регистрам GPIO.
 *
 * На стенде это обычный volatile-доступ по адресу (inline, без накладных
 * расходов). В сборке симулятора те же макросы разворачиваются в вызовы
 * функций, которые перехватывает модель аппаратуры - именно так задание
 * требует реализовать операции "записать в регистр" и "прочитать регистр".
 * Драйвер mygpio при этом остаётся одним и тем же исходным текстом.
 */
#ifdef SIM_BUILD

uint32_t gpio_bus_read32 (const volatile void *addr);
void     gpio_bus_write32(volatile void *addr, uint32_t value);
void     gpio_bus_clock_enable(GPIO_TypeDef *port, int enable);

#else

static inline uint32_t gpio_bus_read32(const volatile void *addr)
{
    return *(const volatile uint32_t *)addr;
}

static inline void gpio_bus_write32(volatile void *addr, uint32_t value)
{
    *(volatile uint32_t *)addr = value;
}

static inline void gpio_bus_clock_enable(GPIO_TypeDef *port, int enable)
{
    const uint32_t index = ((uint32_t)(uintptr_t)port - (uint32_t)GPIOA_BASE) / 0x400u;
    const uint32_t bit   = 1u << index;

    if (enable) {
        RCC->AHB1ENR |= bit;
        /*
         * Errata "Delay after an RCC peripheral clock enabling": между записью
         * в AHB1ENR и первым обращением к регистрам порта нужен такт задержки,
         * его обеспечивает обратное чтение.
         */
        (void)RCC->AHB1ENR;
    } else {
        RCC->AHB1ENR &= ~bit;
    }
}

#endif /* SIM_BUILD */

#define GPIO_REG_RD(port, reg)       gpio_bus_read32(&((port)->reg))
#define GPIO_REG_WR(port, reg, val)  gpio_bus_write32(&((port)->reg), (val))
#define GPIO_AFR_RD(port, idx)       gpio_bus_read32(&((port)->AFR[(idx)]))
#define GPIO_AFR_WR(port, idx, val)  gpio_bus_write32(&((port)->AFR[(idx)]), (val))

#endif /* GPIO_BUS_H */
