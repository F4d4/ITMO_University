#include <stdatomic.h>
#include <string.h>

#include "oled.h"
#include "app_config.h"
#include "board_pins.h"
#include "pca9538.h"
#include "stm32f4xx_hal.h"

/* Управляющий байт перед данными посылки (Co = 0) */
#define CTRL_COMMANDS  0x00u
#define CTRL_DATA      0x40u

#define CHUNK_COUNT    (OLED_FRAME_SIZE / OLED_CHUNK_BYTES)

_Static_assert((OLED_FRAME_SIZE % OLED_CHUNK_BYTES) == 0u, "кадр должен делиться на части без остатка");

/* Инициализация контроллера - таблица 3 пособия, одной посылкой */
static const uint8_t k_init_cmds[] = {
    0xAE,           /* дисплей выключен */
    0x20, 0x00,     /* горизонтальная адресация видеопамяти */
    0xC8,           /* развёртка COM снизу вверх */
    0x40,           /* начальная строка 0 */
    0x81, 0xFF,     /* контраст */
    0xA1,           /* SEG0 - столбец 127 */
    0xA6,           /* нормальное (не инверсное) изображение */
    0xA8, 0x3F,     /* мультиплексор на 64 строки */
    0xA4,           /* вывод содержимого видеопамяти */
    0xD3, 0x00,     /* вертикальный сдвиг 0 */
    0xD5, 0xF0,     /* тактовый генератор */
    0xD9, 0x22,     /* предзаряд */
    0xDA, 0x12,     /* конфигурация выводов COM */
    0xDB, 0x20,     /* уровень Vcomh */
    0x8D, 0x14,     /* встроенный преобразователь напряжения включён */
    0xAF            /* дисплей включён */
};

/* Указатель записи - в начало видеопамяти: столбцы 0..127, страницы 0..7 (таблица 4) */
static const uint8_t k_home_cmds[] = { 0x21, 0x00, 0x7F, 0x22, 0x00, 0x07 };

/*
 * Состояние передачи кадра. Главный цикл меняет только IDLE -> READY (после
 * копирования кадра), прерывание I2C - READY -> SENDING -> IDLE. Каждая
 * сторона трогает состояние только тогда, когда другая его не трогает,
 * поэтому критическая секция не нужна.
 */
typedef enum {
    FRAME_IDLE = 0,
    FRAME_READY,      /* кадр принят; первой уйдёт команда установки адреса */
    FRAME_SENDING     /* адрес установлен, идут части кадра */
} frame_state_t;

static uint8_t                b_tx[OLED_FRAME_SIZE];
static volatile frame_state_t b_state;
static uint16_t               b_chunk;    /* номер части, которая передаётся или уйдёт следующей */

static bool oled_reset(void)
{
    /* Сначала RST# в единице и вывод P0 - выход; остальные выводы остаются входами */
    const uint8_t out_high = 0xFFu;
    const uint8_t out_low  = (uint8_t)~BOARD_IOEXP_OLED_RST_MASK;
    const uint8_t cfg      = (uint8_t)~BOARD_IOEXP_OLED_RST_MASK;

    if (!i2c_bus_write(BOARD_I2C_ADDR_IOEXP, PCA9538_REG_OUTPUT, &out_high, 1u) ||
        !i2c_bus_write(BOARD_I2C_ADDR_IOEXP, PCA9538_REG_CONFIG, &cfg, 1u)) {
        return false;
    }

    /*
     * Импульс нуля: между двумя записями выходного регистра проходит целая
     * транзакция I2C (около 70 мкс), а требуется не меньше 1 мкс.
     */
    if (!i2c_bus_write(BOARD_I2C_ADDR_IOEXP, PCA9538_REG_OUTPUT, &out_low, 1u) ||
        !i2c_bus_write(BOARD_I2C_ADDR_IOEXP, PCA9538_REG_OUTPUT, &out_high, 1u)) {
        return false;
    }

    HAL_Delay(2u);   /* после сброса - пауза около 1 мс */
    return true;
}

bool oled_init(void)
{
    b_state = FRAME_IDLE;
    b_chunk = 0u;

    if (!oled_reset() ||
        !i2c_bus_write(BOARD_I2C_ADDR_OLED, CTRL_COMMANDS, k_init_cmds, sizeof(k_init_cmds)) ||
        !i2c_bus_write(BOARD_I2C_ADDR_OLED, CTRL_COMMANDS, k_home_cmds, sizeof(k_home_cmds))) {
        return false;
    }

    /* После сброса в видеопамяти мусор: очищаем её до включения прерываний */
    memset(b_tx, 0, sizeof(b_tx));
    return i2c_bus_write(BOARD_I2C_ADDR_OLED, CTRL_DATA, b_tx, sizeof(b_tx));
}

bool oled_ready(void)
{
    return b_state == FRAME_IDLE;
}

bool oled_submit(const uint8_t *frame)
{
    if (b_state != FRAME_IDLE) {
        return false;
    }

    memcpy(b_tx, frame, sizeof(b_tx));
    atomic_signal_fence(memory_order_release);   /* кадр скопирован раньше смены состояния */
    b_state = FRAME_READY;
    return true;
}

/* Прерывание I2C: завершилась транзакция передачи кадра */
static void oled_txn_done(bool ok)
{
    if (!ok) {
        /* Указатель записи дисплея мог разойтись с b_chunk: кадр передаётся заново */
        b_state = FRAME_READY;
        return;
    }

    if (b_state == FRAME_READY) {
        b_chunk = 0u;
        b_state = FRAME_SENDING;
    } else if (++b_chunk == CHUNK_COUNT) {
        b_state = FRAME_IDLE;
    }
}

bool oled_next_txn(i2c_txn_t *t)
{
    const frame_state_t state = b_state;

    if (state == FRAME_IDLE) {
        return false;
    }

    t->dev  = BOARD_I2C_ADDR_OLED;
    t->read = false;
    t->done = oled_txn_done;

    if (state == FRAME_READY) {
        t->reg  = CTRL_COMMANDS;
        t->data = (uint8_t *)k_home_cmds;   /* HAL только читает буфер передачи */
        t->len  = sizeof(k_home_cmds);
    } else {
        t->reg  = CTRL_DATA;
        t->data = &b_tx[b_chunk * OLED_CHUNK_BYTES];
        t->len  = OLED_CHUNK_BYTES;
    }
    return true;
}
