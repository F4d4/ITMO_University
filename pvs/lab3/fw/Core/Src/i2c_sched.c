#include <stdbool.h>

#include "i2c_sched.h"
#include "accel.h"
#include "app_config.h"
#include "board.h"
#include "board_pins.h"
#include "i2c_bus.h"
#include "keypad.h"
#include "main.h"
#include "oled.h"
#include "stm32f4xx_hal.h"

/*
 * Цикл расписания - 20 тактов по 1 мс (20 мс):
 *
 *   такт   0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19
 *          K  K  K  K  K  K  K  K  A  D  D  D  D  D  D  D  D  D  D  D
 *
 *   K - клавиатура: 4 строки по 2 транзакции, полный опрос матрицы за цикл;
 *   A - акселерометр: чтение шести байтов ускорений, 50 раз в секунду;
 *   D - дисплей: команда установки адреса видеопамяти или очередная часть
 *       кадра (OLED_CHUNK_BYTES байт). Кадр - 1 + 1024 / 32 = 33 транзакции,
 *       то есть три цикла (60 мс, около 16 кадров в секунду). Если нового
 *       кадра нет, такт D простаивает.
 *
 * Самая длинная транзакция - часть кадра: 34 байта по 9 тактов SCL при
 * 400 кГц, около 0,77 мс, поэтому она укладывается в свой такт.
 */
typedef enum {
    SLOT_KEYPAD = 0,
    SLOT_ACCEL,
    SLOT_OLED
} sched_slot_t;

#define K SLOT_KEYPAD
#define A SLOT_ACCEL
#define D SLOT_OLED

static const uint8_t k_schedule[] = {
    K, K, K, K, K, K, K, K, A, D, D, D, D, D, D, D, D, D, D, D
};

#undef K
#undef A
#undef D

#define SCHED_LEN  (sizeof(k_schedule) / sizeof(k_schedule[0]))

static TIM_HandleTypeDef b_htim;
static uint32_t          b_pos;

volatile i2c_sched_stats_t i2c_sched_stats;

static bool i2c_sched_txn_for(sched_slot_t slot, i2c_txn_t *t)
{
    switch (slot) {
    case SLOT_KEYPAD: return keypad_next_txn(t);
    case SLOT_ACCEL:  return accel_next_txn(t);
    case SLOT_OLED:   return oled_next_txn(t);
    default:          return false;
    }
}

/* Один такт расписания; выполняется в прерывании таймера */
static void i2c_sched_tick(void)
{
    const sched_slot_t slot = (sched_slot_t)k_schedule[b_pos];

    b_pos = (b_pos + 1u < SCHED_LEN) ? (b_pos + 1u) : 0u;
    i2c_sched_stats.ticks++;

    i2c_bus_watchdog(HAL_GetTick());
    if (i2c_bus_busy()) {
        i2c_sched_stats.busy_skips++;
        return;
    }

    i2c_txn_t t;

    if (i2c_sched_txn_for(slot, &t)) {
        (void)i2c_bus_submit(&t);
    }
}

void i2c_sched_start(void)
{
    const uint32_t clk_hz = board_sched_tim_clock_hz();

    board_sched_tim_clk_enable();

    /* Счётчик тактируется частотой 1 МГц, переполнение - раз в SCHED_TICK_US */
    b_htim.Instance               = board_sched_tim_regs();
    b_htim.Init.Prescaler         = clk_hz / 1000000u - 1u;
    b_htim.Init.CounterMode       = TIM_COUNTERMODE_UP;
    b_htim.Init.Period            = SCHED_TICK_US - 1u;
    b_htim.Init.ClockDivision     = TIM_CLOCKDIVISION_DIV1;
    b_htim.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_DISABLE;

    if (HAL_TIM_Base_Init(&b_htim) != HAL_OK) {
        Error_Handler();
    }

    b_pos = 0u;
    HAL_NVIC_SetPriority(board_sched_tim_irqn(), SCHED_IRQ_PRIORITY, 0u);
    HAL_NVIC_EnableIRQ(board_sched_tim_irqn());

    if (HAL_TIM_Base_Start_IT(&b_htim) != HAL_OK) {
        Error_Handler();
    }
}

void HAL_TIM_PeriodElapsedCallback(TIM_HandleTypeDef *htim)
{
    if (htim == &b_htim) {
        i2c_sched_tick();
    }
}

void BOARD_SCHED_TIM_IRQHandler(void)
{
    HAL_TIM_IRQHandler(&b_htim);
}
