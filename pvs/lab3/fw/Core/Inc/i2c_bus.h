#ifndef I2C_BUS_H
#define I2C_BUS_H

#include <stdbool.h>
#include <stdint.h>

/*
 * Шина I2C стенда поверх стандартного драйвера HAL.
 *
 * Два режима работы:
 *  - по опросу (i2c_bus_write/read) - только при начальной инициализации
 *    устройств, пока прерывания I2C запрещены;
 *  - по прерываниям (i2c_bus_submit) - после i2c_bus_start_async(). Новую
 *    транзакцию запускает только планировщик (i2c_sched) из прерывания
 *    своего таймера и только когда предыдущая завершилась, поэтому
 *    транзакции разных устройств на шине никогда не пересекаются.
 *
 * Все транзакции регистровые: после адреса устройства передаётся один байт
 * адреса регистра (у дисплея на его месте управляющий байт 0x00/0x40).
 */

/* Вызывается из прерывания I2C по завершении транзакции */
typedef void (*i2c_done_fn)(bool ok);

typedef struct {
    uint8_t     dev;    /* 8-битный адрес устройства */
    uint8_t     reg;    /* адрес регистра или управляющий байт */
    bool        read;
    uint8_t    *data;
    uint16_t    len;
    i2c_done_fn done;
} i2c_txn_t;

/* Счётчики для проверки расписания отладчиком */
typedef struct {
    uint32_t started;
    uint32_t failed;     /* NACK, ошибка шины, потеря арбитража */
    uint32_t timeouts;   /* шина зависла и была перезапущена */
    uint32_t max_us;     /* самая долгая транзакция */
} i2c_bus_stats_t;

extern volatile i2c_bus_stats_t i2c_bus_stats;

/* Освободить шину, настроить блок I2C; прерывания остаются запрещены */
void i2c_bus_init(void);

/* --- Обмен по опросу (до i2c_bus_start_async) --- */
bool i2c_bus_write(uint8_t dev, uint8_t reg, const uint8_t *data, uint16_t len);
bool i2c_bus_read (uint8_t dev, uint8_t reg, uint8_t *data, uint16_t len);

/* --- Обмен по прерываниям --- */
void i2c_bus_start_async(void);

bool i2c_bus_busy(void);

/*
 * Запустить транзакцию. false - шина занята или HAL отказал; в этом случае
 * t->done не вызывается. Если HAL отказал из-за зависшей шины (флаг BUSY не
 * снимается), шина перезапускается.
 */
bool i2c_bus_submit(const i2c_txn_t *t);

/*
 * Проверка зависания; вызывается планировщиком на каждом такте. Если
 * транзакция идёт дольше I2C_TXN_TIMEOUT_MS, она прерывается с ошибкой,
 * а шина и блок I2C инициализируются заново.
 */
void i2c_bus_watchdog(uint32_t now_ms);

#endif /* I2C_BUS_H */
