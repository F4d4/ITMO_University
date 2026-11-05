#include "keypad.h"
#include "app_config.h"
#include "board_pins.h"
#include "fifo.h"
#include "pca9538.h"

#define KEY_COUNT  (BOARD_KEYPAD_ROWS * BOARD_KEYPAD_COLS)

/*
 * Опрос строки - две транзакции: запись регистра конфигурации (строка
 * становится выходом, остальные строки остаются входами) и чтение входного
 * порта. В выходном регистре нули записаны при инициализации, поэтому
 * строка, ставшая выходом, сразу выдаёт "0". Нажатая кнопка этой строки
 * замыкает свой столбец на ноль; столбцы подтянуты к единице.
 */
typedef enum {
    STEP_SELECT_ROW = 0,
    STEP_READ_COLS
} kbd_step_t;

/* --- Состояние опроса; меняется только в прерывании I2C --- */
static kbd_step_t b_step;
static uint8_t    b_row;
static uint8_t    b_cfg;          /* значение для регистра конфигурации */
static uint8_t    b_in;           /* прочитанный входной порт */
static uint16_t   b_raw;          /* матрица текущего опроса: бит (код - 1) */
static uint8_t    b_cnt[KEY_COUNT];
static uint16_t   b_down;         /* кнопки, признанные нажатыми */
static uint8_t    b_active;       /* код единственной нажатой кнопки, 0 - нет */

/* Нажатия: пишет прерывание I2C, читает главный цикл */
static uint8_t    b_queue_mem[FIFO_STORAGE_SIZE(KBD_EVENT_QUEUE)];
static fifo_t     b_queue;

bool keypad_init(void)
{
    const uint8_t out_zero  = 0x00u;
    const uint8_t all_input = 0xFFu;

    b_step   = STEP_SELECT_ROW;
    b_row    = 0u;
    b_raw    = 0u;
    b_down   = 0u;
    b_active = 0u;
    for (uint32_t i = 0u; i < KEY_COUNT; ++i) {
        b_cnt[i] = 0u;
    }
    fifo_init(&b_queue, b_queue_mem, (uint16_t)sizeof(b_queue_mem));

    return i2c_bus_write(BOARD_I2C_ADDR_KEYPAD, PCA9538_REG_CONFIG, &all_input, 1u)
        && i2c_bus_write(BOARD_I2C_ADDR_KEYPAD, PCA9538_REG_OUTPUT, &out_zero, 1u);
}

/* Конец полного опроса матрицы: шаг антидребезга для каждой кнопки */
static void keypad_scan_done(uint16_t raw)
{
    for (uint32_t i = 0u; i < KEY_COUNT; ++i) {
        const uint16_t bit = (uint16_t)(1u << i);

        if ((raw & bit) != 0u) {
            if (b_cnt[i] < KBD_CNT_MAX) {
                b_cnt[i]++;
            }
        } else if (b_cnt[i] > 0u) {
            b_cnt[i]--;
        }

        if (((b_down & bit) == 0u) && (b_cnt[i] >= KBD_CNT_PRESS)) {
            b_down |= bit;
        } else if (((b_down & bit) != 0u) && (b_cnt[i] <= KBD_CNT_RELEASE)) {
            b_down &= (uint16_t)~bit;
        }
    }

    /* Ровно одна нажатая кнопка - её код; ни одной или несколько - 0 */
    uint8_t active = 0u;

    if ((b_down != 0u) && ((b_down & (b_down - 1u)) == 0u)) {
        while ((b_down & (1u << active)) == 0u) {
            active++;
        }
        active++;
    }

    /* Событие только при смене состояния: удержание повторов не даёт */
    if ((active != 0u) && (active != b_active)) {
        (void)fifo_put(&b_queue, active);
    }
    b_active = active;
}

/* Прерывание I2C: завершилась транзакция опроса */
static void keypad_txn_done(bool ok)
{
    if (!ok) {
        /* Опрос с ошибкой не учитывается: начинаем матрицу заново */
        b_step = STEP_SELECT_ROW;
        b_row  = 0u;
        b_raw  = 0u;
        return;
    }

    if (b_step == STEP_SELECT_ROW) {
        b_step = STEP_READ_COLS;
        return;
    }

    /* Нажатая кнопка даёт ноль в своём столбце */
    const uint8_t closed = (uint8_t)~b_in;
    const uint8_t cols   = (uint8_t)((closed >> BOARD_KEYPAD_COL_SHIFT) & ((1u << BOARD_KEYPAD_COLS) - 1u));

    b_raw |= (uint16_t)((uint16_t)cols << (b_row * BOARD_KEYPAD_COLS));
    b_step = STEP_SELECT_ROW;

    if (++b_row == BOARD_KEYPAD_ROWS) {
        keypad_scan_done(b_raw);
        b_row = 0u;
        b_raw = 0u;
    }
}

bool keypad_next_txn(i2c_txn_t *t)
{
    t->dev  = BOARD_I2C_ADDR_KEYPAD;
    t->len  = 1u;
    t->done = keypad_txn_done;

    if (b_step == STEP_SELECT_ROW) {
        b_cfg   = (uint8_t)~(1u << (BOARD_KEYPAD_ROW_SHIFT + b_row));
        t->reg  = PCA9538_REG_CONFIG;
        t->read = false;
        t->data = &b_cfg;
    } else {
        t->reg  = PCA9538_REG_INPUT;
        t->read = true;
        t->data = &b_in;
    }
    return true;
}

bool keypad_pop(uint8_t *code)
{
    return fifo_get(&b_queue, code);
}
