#include "gpio_hw.h"

#ifdef GPIO_BACKEND_MY

#include "mygpio.h"

/*
 * Реализация абстрактного интерфейса выводов поверх собственного драйвера
 * GPIO (часть 2, способ 1). Переходник тонкий: вся содержательная работа с
 * регистрами выполняется в mygpio.c.
 */

void gpio_hw_port_enable(gpio_hw_port_t port)
{
    (void)mygpio_port_clock_enable(port);
}

void gpio_hw_config_mask(gpio_hw_port_t port, uint16_t mask,
                         gpio_hw_mode_t mode, gpio_hw_pull_t pull,
                         uint8_t init_level)
{
    mygpio_cfg_t cfg;

    switch (mode) {
    case GPIO_HW_OUT_PP:
        cfg.mode  = MYGPIO_MODE_OUTPUT;
        cfg.otype = MYGPIO_OTYPE_PP;
        break;
    case GPIO_HW_OUT_OD:
        cfg.mode  = MYGPIO_MODE_OUTPUT;
        cfg.otype = MYGPIO_OTYPE_OD;
        break;
    case GPIO_HW_ANALOG:
        cfg.mode  = MYGPIO_MODE_ANALOG;
        cfg.otype = MYGPIO_OTYPE_PP;
        break;
    case GPIO_HW_IN:
    default:
        cfg.mode  = MYGPIO_MODE_INPUT;
        cfg.otype = MYGPIO_OTYPE_PP;
        break;
    }

    switch (pull) {
    case GPIO_HW_PULLUP:   cfg.pull = MYGPIO_PULL_UP;   break;
    case GPIO_HW_PULLDOWN: cfg.pull = MYGPIO_PULL_DOWN; break;
    case GPIO_HW_NOPULL:
    default:               cfg.pull = MYGPIO_PULL_NONE; break;
    }

    cfg.speed      = MYGPIO_SPEED_LOW;   /* индикация не требует крутых фронтов */
    cfg.af         = 0u;
    cfg.init_level = (init_level == GPIO_HW_LEVEL_KEEP) ? MYGPIO_LEVEL_KEEP : init_level;

    (void)mygpio_init_mask(port, mask, &cfg);
}

uint16_t gpio_hw_read_mask(gpio_hw_port_t port, uint16_t mask)
{
    return mygpio_read_mask(port, mask);
}

uint16_t gpio_hw_read_out_mask(gpio_hw_port_t port, uint16_t mask)
{
    return mygpio_read_out_mask(port, mask);
}

void gpio_hw_write_mask(gpio_hw_port_t port, uint16_t mask, uint16_t values)
{
    mygpio_write_mask(port, mask, values);
}

#endif /* GPIO_BACKEND_MY */
