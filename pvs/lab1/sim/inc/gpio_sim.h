#ifndef GPIO_SIM_H
#define GPIO_SIM_H

#include <stdint.h>
#include <stdbool.h>

/*
 * Программная модель блока управления портами ввода-вывода STM32F427
 * (часть 2, способ 2).
 *
 * Моделируются управляющие регистры GPIO и логика, связывающая их с
 * физическими выводами. Настройка синхросигналов не моделируется: считается,
 * что готовый синхросигнал подаётся извне, а модельное время продвигает
 * вызывающая программа.
 */

#define SIM_PORT_COUNT  5u    /* GPIOA..GPIOE */
#define SIM_PIN_COUNT  16u

/* Смещения регистров относительно базового адреса порта */
#define SIM_REG_MODER    0x00u
#define SIM_REG_OTYPER   0x04u
#define SIM_REG_OSPEEDR  0x08u
#define SIM_REG_PUPDR    0x0Cu
#define SIM_REG_IDR      0x10u
#define SIM_REG_ODR      0x14u
#define SIM_REG_BSRR     0x18u
#define SIM_REG_LCKR     0x1Cu
#define SIM_REG_AFRL     0x20u
#define SIM_REG_AFRH     0x24u

/* Чем внешняя цепь воздействует на вывод */
typedef enum {
    PAD_Z = 0,     /* внешняя цепь вывод не трогает */
    PAD_LOW,
    PAD_HIGH
} pad_drive_t;

/* Результирующий уровень на выводе */
typedef enum {
    PAD_LEVEL_FLOAT = 0,   /* никто не задаёт уровень      */
    PAD_LEVEL_LOW,
    PAD_LEVEL_HIGH,
    PAD_LEVEL_X            /* конфликт источников          */
} pad_level_t;

void gpio_sim_reset(void);

/* Операции "прочитать регистр" и "записать регистр" */
uint32_t gpio_sim_read_reg (uint8_t port, uint32_t offset);
void     gpio_sim_write_reg(uint8_t port, uint32_t offset, uint32_t value);

/* Те же операции, адресуемые как обращения по шине */
uint32_t gpio_sim_read32 (uintptr_t address);
void     gpio_sim_write32(uintptr_t address, uint32_t value);

/* Тактирование порта (подаётся извне модели) */
void gpio_sim_clock_enable(uint8_t port, bool enable);
bool gpio_sim_clock_enabled(uint8_t port);

/* Внешний мир: программируемое воздействие на входные выводы */
void        gpio_sim_set_pad  (uint8_t port, uint8_t pin, pad_drive_t drive, const char *note);
pad_level_t gpio_sim_pad_level(uint8_t port, uint8_t pin);

/* Счётчики нарушений, обнаруженных моделью */
uint32_t gpio_sim_violations(void);

/* Число изменений уровня на выводе - мера выбросов при реинициализации */
uint32_t gpio_sim_pad_transitions(uint8_t port, uint8_t pin);

/* Разбор адреса шины; возвращает false, если адрес не принадлежит GPIO */
bool gpio_sim_decode(uintptr_t address, uint8_t *port, uint32_t *offset);

#endif /* GPIO_SIM_H */
