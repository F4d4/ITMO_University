#include "naive_gpio.h"
#include "gpio_bus.h"

void naive_gpio_init_output(GPIO_TypeDef *port, uint8_t pin, uint8_t level)
{
    /* Сначала включаем выходной каскад - здесь и возникает выброс:
       на площадку немедленно попадает то, что осталось в защёлке ODR. */
    uint32_t moder = GPIO_REG_RD(port, MODER);
    moder &= ~(3u << (pin * 2u));
    moder |=  (1u << (pin * 2u));
    GPIO_REG_WR(port, MODER, moder);

    GPIO_REG_WR(port, OSPEEDR, GPIO_REG_RD(port, OSPEEDR) & ~(3u << (pin * 2u)));
    GPIO_REG_WR(port, OTYPER,  GPIO_REG_RD(port, OTYPER)  & ~(1u << pin));
    GPIO_REG_WR(port, PUPDR,   GPIO_REG_RD(port, PUPDR)   & ~(3u << (pin * 2u)));

    /* И только теперь задаём нужный уровень */
    GPIO_REG_WR(port, BSRR, (level != 0u) ? (1u << pin) : (1u << (pin + 16u)));
}
