#ifndef ACCEL_H
#define ACCEL_H

#include <stdbool.h>
#include <stdint.h>

#include "i2c_bus.h"

/*
 * Акселерометр LSM9DS1 (без гироскопа и магнитометра): диапазон +-2g,
 * 119 отсчётов в секунду, полоса фильтра 50 Гц.
 */

typedef struct {
    int16_t  mg[3];     /* ускорение по осям X, Y, Z датчика, mg */
    uint32_t t_ms;      /* когда отсчёт прочитан */
} accel_sample_t;

/* Проверка WHO_AM_I и настройка; обмен по опросу. false - датчик не отвечает */
bool accel_init(void);

/* Транзакция чтения ускорений для планировщика (прерывание таймера) */
bool accel_next_txn(i2c_txn_t *t);

/* Последний прочитанный отсчёт; false - ни одного ещё не было */
bool accel_get(accel_sample_t *out);

#endif /* ACCEL_H */
