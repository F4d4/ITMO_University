#include "gpio_hw.h"
#include "stm32f4xx_hal.h"

/*
 * Реализация абстрактного интерфейса выводов поверх стандартного драйвера
 * HAL GPIO (перенесена из лабораторной работы 1).
 */

static void gpio_hw_clock_enable(gpio_hw_port_t port)
{
    if      (port == GPIOA) { __HAL_RCC_GPIOA_CLK_ENABLE(); }
    else if (port == GPIOB) { __HAL_RCC_GPIOB_CLK_ENABLE(); }
    else if (port == GPIOC) { __HAL_RCC_GPIOC_CLK_ENABLE(); }
    else if (port == GPIOD) { __HAL_RCC_GPIOD_CLK_ENABLE(); }
    else if (port == GPIOE) { __HAL_RCC_GPIOE_CLK_ENABLE(); }
}

static uint32_t gpio_hw_hal_pull(gpio_hw_pull_t pull)
{
    switch (pull) {
    case GPIO_HW_PULLUP:   return GPIO_PULLUP;
    case GPIO_HW_PULLDOWN: return GPIO_PULLDOWN;
    case GPIO_HW_NOPULL:
    default:               return GPIO_NOPULL;
    }
}

void gpio_hw_port_enable(gpio_hw_port_t port)
{
    gpio_hw_clock_enable(port);
}

void gpio_hw_config_mask(gpio_hw_port_t port, uint16_t mask,
                         gpio_hw_mode_t mode, gpio_hw_pull_t pull,
                         uint8_t init_level)
{
    GPIO_InitTypeDef init = { 0 };

    /*
     * HAL не выполняет предзагрузку защёлки ODR внутри HAL_GPIO_Init(), это
     * забота вызывающего. Поэтому уровень выставляется здесь, до перевода
     * вывода в режим выхода, - иначе на выводе возник бы кратковременный
     * выброс содержимым ODR, оставшимся от предыдущей настройки.
     */
    if ((init_level != GPIO_HW_LEVEL_KEEP) &&
        ((mode == GPIO_HW_OUT_PP) || (mode == GPIO_HW_OUT_OD))) {
        HAL_GPIO_WritePin(port, mask, (init_level != 0u) ? GPIO_PIN_SET : GPIO_PIN_RESET);
    }

    switch (mode) {
    case GPIO_HW_OUT_PP:  init.Mode = GPIO_MODE_OUTPUT_PP; break;
    case GPIO_HW_OUT_OD:  init.Mode = GPIO_MODE_OUTPUT_OD; break;
    case GPIO_HW_ANALOG:  init.Mode = GPIO_MODE_ANALOG;    break;
    case GPIO_HW_IN:
    default:              init.Mode = GPIO_MODE_INPUT;     break;
    }

    init.Pin   = mask;
    init.Pull  = gpio_hw_hal_pull(pull);
    init.Speed = GPIO_SPEED_FREQ_LOW;

    HAL_GPIO_Init(port, &init);
}

void gpio_hw_config_af(gpio_hw_port_t port, uint16_t mask,
                       uint8_t af, gpio_hw_pull_t pull)
{
    GPIO_InitTypeDef init = { 0 };

    init.Pin       = mask;
    init.Mode      = GPIO_MODE_AF_PP;
    init.Pull      = gpio_hw_hal_pull(pull);
    /* Фронты сигнала 115200 бит/с не требуют высокой скорости выходного каскада */
    init.Speed     = GPIO_SPEED_FREQ_MEDIUM;
    init.Alternate = af;

    HAL_GPIO_Init(port, &init);
}

uint16_t gpio_hw_read_mask(gpio_hw_port_t port, uint16_t mask)
{
    return (uint16_t)(port->IDR & (uint32_t)mask);
}

uint16_t gpio_hw_read_out_mask(gpio_hw_port_t port, uint16_t mask)
{
    return (uint16_t)(port->ODR & (uint32_t)mask);
}

void gpio_hw_write_mask(gpio_hw_port_t port, uint16_t mask, uint16_t values)
{
    /*
     * В HAL нет функции, задающей произвольный узор уровней на группе выводов
     * за одну операцию: HAL_GPIO_WritePin() назначает всем выводам маски один
     * и тот же уровень. Два последовательных вызова для двухцветного
     * светодиода дали бы между ними промежуточное состояние - вспышку не того
     * цвета. Поэтому здесь выполняется прямая запись в BSRR.
     */
    const uint32_t bsrr = (uint32_t)(uint16_t)(mask &  values)
                        | ((uint32_t)(uint16_t)(mask & (uint16_t)~values) << 16);

    port->BSRR = bsrr;
}
