#ifndef MYGPIO_H
#define MYGPIO_H

#include <stdint.h>
#include <stdbool.h>
#include "gpio_regs.h"

/*
 * Собственный драйвер портов ввода-вывода общего назначения (часть 2, способ 1).
 *
 * Из стандартных библиотек используется только описание структуры регистров
 * GPIO_TypeDef и define-определения адресов - это разрешено заданием.
 * Ни одна функция и ни одна структура HAL/LL для работы с GPIO не задействована.
 *
 * Назначение полей регистров - по RM0090, раздел "General purpose I/O (GPIO)".
 */

typedef enum {
    MYGPIO_MODE_INPUT  = 0u,   /* MODER = 00 */
    MYGPIO_MODE_OUTPUT = 1u,   /* MODER = 01 */
    MYGPIO_MODE_AF     = 2u,   /* MODER = 10 */
    MYGPIO_MODE_ANALOG = 3u    /* MODER = 11 */
} mygpio_mode_t;

typedef enum {
    MYGPIO_OTYPE_PP = 0u,      /* OTYPER = 0, двухтактный выход */
    MYGPIO_OTYPE_OD = 1u       /* OTYPER = 1, открытый сток     */
} mygpio_otype_t;

typedef enum {
    MYGPIO_SPEED_LOW       = 0u,
    MYGPIO_SPEED_MEDIUM    = 1u,
    MYGPIO_SPEED_HIGH      = 2u,
    MYGPIO_SPEED_VERY_HIGH = 3u
} mygpio_speed_t;

typedef enum {
    MYGPIO_PULL_NONE = 0u,
    MYGPIO_PULL_UP   = 1u,
    MYGPIO_PULL_DOWN = 2u
} mygpio_pull_t;

/* Не менять текущий уровень на выводе при реинициализации */
#define MYGPIO_LEVEL_KEEP  0xFFu

typedef struct {
    mygpio_mode_t  mode;
    mygpio_otype_t otype;
    mygpio_speed_t speed;
    mygpio_pull_t  pull;
    uint8_t        af;          /* 0..15, значим только при MYGPIO_MODE_AF */
    uint8_t        init_level;  /* 0, 1 или MYGPIO_LEVEL_KEEP */
} mygpio_cfg_t;

typedef enum {
    MYGPIO_OK = 0,
    MYGPIO_ERR_PARAM,
    MYGPIO_ERR_LOCKED
} mygpio_status_t;

/* --- Тактирование порта --- */
mygpio_status_t mygpio_port_clock_enable (GPIO_TypeDef *port);
mygpio_status_t mygpio_port_clock_disable(GPIO_TypeDef *port);

/* --- Конфигурация режимов работы --- */
mygpio_status_t mygpio_init_pin   (GPIO_TypeDef *port, uint8_t  pin,  const mygpio_cfg_t *cfg);
mygpio_status_t mygpio_init_mask  (GPIO_TypeDef *port, uint16_t mask, const mygpio_cfg_t *cfg);
mygpio_status_t mygpio_deinit_mask(GPIO_TypeDef *port, uint16_t mask);

/* --- Получение текущих настроек режима работы --- */
mygpio_status_t mygpio_get_config (GPIO_TypeDef *port, uint8_t pin, mygpio_cfg_t *out);

/* --- Чтение входных значений --- */
uint8_t  mygpio_read_pin     (GPIO_TypeDef *port, uint8_t  pin);
uint16_t mygpio_read_port    (GPIO_TypeDef *port);
uint16_t mygpio_read_mask    (GPIO_TypeDef *port, uint16_t mask);

/* --- Чтение защёлкнутых выходных значений --- */
uint8_t  mygpio_read_out_pin (GPIO_TypeDef *port, uint8_t  pin);
uint16_t mygpio_read_out_mask(GPIO_TypeDef *port, uint16_t mask);

/* --- Запись выходных значений --- */
void mygpio_write_pin  (GPIO_TypeDef *port, uint8_t  pin,  uint8_t  level);
void mygpio_set_mask   (GPIO_TypeDef *port, uint16_t mask);
void mygpio_clear_mask (GPIO_TypeDef *port, uint16_t mask);
void mygpio_toggle_mask(GPIO_TypeDef *port, uint16_t mask);
void mygpio_write_port (GPIO_TypeDef *port, uint16_t value);

/*
 * Одновременная запись произвольного набора значений в пределах одного порта:
 * все выводы маски принимают новые уровни за одну 32-разрядную запись в BSRR.
 */
void mygpio_write_mask (GPIO_TypeDef *port, uint16_t mask, uint16_t values);

/* --- Блокировка конфигурации (LCKR) --- */
mygpio_status_t mygpio_lock_mask  (GPIO_TypeDef *port, uint16_t mask);
uint16_t        mygpio_locked_mask(GPIO_TypeDef *port);

#endif /* MYGPIO_H */
