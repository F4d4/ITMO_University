#include <stdint.h>

#include "gpio_bus.h"
#include "gpio_sim.h"

/*
 * Подмена шины доступа к регистрам в сборке симулятора.
 * Драйвер mygpio обращается к регистрам только через эти функции, поэтому
 * модель аппаратуры видит каждое чтение и каждую запись.
 */

uint32_t gpio_bus_read32(const volatile void *address)
{
    return gpio_sim_read32((uintptr_t)address);
}

void gpio_bus_write32(volatile void *address, uint32_t value)
{
    gpio_sim_write32((uintptr_t)address, value);
}

void gpio_bus_clock_enable(GPIO_TypeDef *port, int enable)
{
    uint8_t  index  = 0u;
    uint32_t offset = 0u;

    if (gpio_sim_decode((uintptr_t)port, &index, &offset)) {
        gpio_sim_clock_enable(index, enable != 0);
    }
}
