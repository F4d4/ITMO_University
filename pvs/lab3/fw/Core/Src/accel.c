#include <stdatomic.h>

#include "accel.h"
#include "board_pins.h"
#include "stm32f4xx_hal.h"

/* Регистры LSM9DS1 (DocID025715, раздел 7) */
#define REG_WHO_AM_I      0x0Fu
#define REG_CTRL_REG6_XL  0x20u
#define REG_CTRL_REG8     0x22u
#define REG_OUT_X_L_XL    0x28u

#define WHO_AM_I_VALUE    0x68u

#define CTRL8_BDU         0x40u   /* старший и младший байты из одного отсчёта */
#define CTRL8_IF_ADD_INC  0x04u   /* автоинкремент адреса при чтении нескольких байтов */
#define CTRL8_SW_RESET    0x01u

/*
 * ODR_XL = 011 (119 Гц), FS_XL = 00 (+-2g), BW_SCAL_ODR = 0: полоса
 * определяется частотой отсчётов и равна 50 Гц. Запись только в
 * CTRL_REG6_XL включает один акселерометр, гироскоп остаётся выключен.
 */
#define CTRL6_XL_VALUE    0x60u

/* Чувствительность при +-2g: 0,061 mg на единицу младшего разряда */
#define MG_PER_LSB_X1000  61

static uint8_t           b_rx[6];
static int16_t           b_mg[3];
static uint32_t          b_t_ms;
static volatile uint32_t b_seq;   /* число опубликованных отсчётов */

bool accel_init(void)
{
    uint8_t v = 0u;

    if (!i2c_bus_read(BOARD_I2C_ADDR_ACCEL, REG_WHO_AM_I, &v, 1u) || (v != WHO_AM_I_VALUE)) {
        return false;
    }

    /* Датчик питается от стенда и помнит настройки прошлого запуска программы */
    v = CTRL8_IF_ADD_INC | CTRL8_SW_RESET;
    if (!i2c_bus_write(BOARD_I2C_ADDR_ACCEL, REG_CTRL_REG8, &v, 1u)) {
        return false;
    }
    HAL_Delay(2u);

    v = CTRL8_BDU | CTRL8_IF_ADD_INC;
    if (!i2c_bus_write(BOARD_I2C_ADDR_ACCEL, REG_CTRL_REG8, &v, 1u)) {
        return false;
    }

    v = CTRL6_XL_VALUE;
    return i2c_bus_write(BOARD_I2C_ADDR_ACCEL, REG_CTRL_REG6_XL, &v, 1u);
}

/* Прерывание I2C: шесть байтов X, Y, Z прочитаны */
static void accel_rx_done(bool ok)
{
    if (!ok) {
        return;
    }

    for (uint32_t i = 0u; i < 3u; ++i) {
        const int16_t raw = (int16_t)((uint16_t)b_rx[2u * i] | ((uint16_t)b_rx[2u * i + 1u] << 8));

        b_mg[i] = (int16_t)((int32_t)raw * MG_PER_LSB_X1000 / 1000);
    }
    b_t_ms = HAL_GetTick();
    atomic_signal_fence(memory_order_release);   /* данные записаны раньше номера */
    b_seq++;
}

bool accel_next_txn(i2c_txn_t *t)
{
    t->dev  = BOARD_I2C_ADDR_ACCEL;
    t->reg  = REG_OUT_X_L_XL;
    t->read = true;
    t->data = b_rx;
    t->len  = sizeof(b_rx);
    t->done = accel_rx_done;
    return true;
}

bool accel_get(accel_sample_t *out)
{
    uint32_t seq;

    /*
     * Отсчёт публикует прерывание: если оно пришло во время копирования,
     * номер изменится, и копирование повторяется.
     */
    do {
        seq = b_seq;
        atomic_signal_fence(memory_order_acquire);
        out->mg[0] = b_mg[0];
        out->mg[1] = b_mg[1];
        out->mg[2] = b_mg[2];
        out->t_ms  = b_t_ms;
        atomic_signal_fence(memory_order_acquire);
    } while (seq != b_seq);

    return seq != 0u;
}
