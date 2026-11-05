#include <stddef.h>

#include "i2c_bus.h"
#include "app_config.h"
#include "board.h"
#include "board_pins.h"
#include "main.h"
#include "stm32f4xx_hal.h"

/*
 * Шина I2C на основе стандартного драйвера HAL (HAL_I2C_Mem_*).
 *
 * Обмен по прерываниям. Транзакцию запускает i2c_bus_submit() из
 * прерывания таймера расписания; HAL ведёт её в прерываниях I2C и по
 * завершении вызывает HAL_I2C_MemTxCpltCallback / MemRxCpltCallback или
 * HAL_I2C_ErrorCallback. Там вызывается обработчик завершения, указанный
 * в описании транзакции, и только после этого шина объявляется свободной.
 *
 * Прерывания I2C приоритетнее прерывания таймера расписания, поэтому, пока
 * идёт обработчик завершения, новая транзакция запуститься не может.
 */

static I2C_HandleTypeDef b_hi2c;

static i2c_txn_t         b_cur;          /* транзакция, которая сейчас идёт */
static volatile bool     b_busy;
static bool              b_async;        /* прерывания I2C разрешены */
static uint32_t          b_start_ms;
static uint32_t          b_start_cycles;

volatile i2c_bus_stats_t i2c_bus_stats;

static void i2c_bus_restart(void);

/* Вызывается из HAL_I2C_Init(): тактирование, выводы, приоритеты прерываний */
void HAL_I2C_MspInit(I2C_HandleTypeDef *hi2c)
{
    if (hi2c != &b_hi2c) {
        return;
    }

    board_i2c_hw_init();

    HAL_NVIC_SetPriority(board_i2c_ev_irqn(), I2C_IRQ_PRIORITY, 0u);
    HAL_NVIC_SetPriority(board_i2c_er_irqn(), I2C_IRQ_PRIORITY, 0u);
}

static void i2c_bus_irq_enable(bool on)
{
    if (on) {
        HAL_NVIC_EnableIRQ(board_i2c_ev_irqn());
        HAL_NVIC_EnableIRQ(board_i2c_er_irqn());
    } else {
        HAL_NVIC_DisableIRQ(board_i2c_ev_irqn());
        HAL_NVIC_DisableIRQ(board_i2c_er_irqn());
    }
}

static void i2c_bus_hw_setup(void)
{
    board_i2c_bus_recover();

    b_hi2c.Instance             = board_i2c_regs();
    b_hi2c.Init.ClockSpeed      = I2C_BUS_SPEED_HZ;
    b_hi2c.Init.DutyCycle       = I2C_DUTYCYCLE_2;
    b_hi2c.Init.OwnAddress1     = 0u;
    b_hi2c.Init.AddressingMode  = I2C_ADDRESSINGMODE_7BIT;
    b_hi2c.Init.DualAddressMode = I2C_DUALADDRESS_DISABLE;
    b_hi2c.Init.OwnAddress2     = 0u;
    b_hi2c.Init.GeneralCallMode = I2C_GENERALCALL_DISABLE;
    b_hi2c.Init.NoStretchMode   = I2C_NOSTRETCH_DISABLE;

    /* Аналоговый фильтр после сброса включён, цифровой выключен - как в пособии */
    if (HAL_I2C_Init(&b_hi2c) != HAL_OK) {
        Error_Handler();
    }
}

void i2c_bus_init(void)
{
    b_busy  = false;
    b_async = false;
    i2c_bus_hw_setup();
    i2c_bus_irq_enable(false);
}

bool i2c_bus_write(uint8_t dev, uint8_t reg, const uint8_t *data, uint16_t len)
{
    /* HAL не меняет передаваемые данные, но объявляет указатель неконстантным */
    return HAL_I2C_Mem_Write(&b_hi2c, dev, reg, I2C_MEMADD_SIZE_8BIT,
                             (uint8_t *)data, len, I2C_BLOCKING_TIMEOUT_MS) == HAL_OK;
}

bool i2c_bus_read(uint8_t dev, uint8_t reg, uint8_t *data, uint16_t len)
{
    return HAL_I2C_Mem_Read(&b_hi2c, dev, reg, I2C_MEMADD_SIZE_8BIT,
                            data, len, I2C_BLOCKING_TIMEOUT_MS) == HAL_OK;
}

void i2c_bus_start_async(void)
{
    b_async = true;
    HAL_NVIC_ClearPendingIRQ(board_i2c_ev_irqn());
    HAL_NVIC_ClearPendingIRQ(board_i2c_er_irqn());
    i2c_bus_irq_enable(true);
}

bool i2c_bus_busy(void)
{
    return b_busy;
}

bool i2c_bus_submit(const i2c_txn_t *t)
{
    if (b_busy) {
        return false;
    }

    b_cur          = *t;
    b_busy         = true;
    b_start_ms     = HAL_GetTick();
    b_start_cycles = board_cycles();

    const HAL_StatusTypeDef st = t->read
        ? HAL_I2C_Mem_Read_IT (&b_hi2c, t->dev, t->reg, I2C_MEMADD_SIZE_8BIT, t->data, t->len)
        : HAL_I2C_Mem_Write_IT(&b_hi2c, t->dev, t->reg, I2C_MEMADD_SIZE_8BIT, t->data, t->len);

    if (st != HAL_OK) {
        b_busy = false;
        i2c_bus_stats.failed++;
        if (st == HAL_ERROR) {
            /* HAL так и не дождался снятия флага BUSY: шина или блок I2C зависли */
            i2c_bus_restart();
        }
        return false;
    }
    i2c_bus_stats.started++;
    return true;
}

/* Завершение транзакции; выполняется в прерывании I2C */
static void i2c_bus_finish(bool ok)
{
    if (!b_busy) {
        return;
    }

    const uint32_t us = (uint32_t)(board_cycles() - b_start_cycles) / board_cycles_per_us();

    if (us > i2c_bus_stats.max_us) {
        i2c_bus_stats.max_us = us;
    }
    if (!ok) {
        i2c_bus_stats.failed++;
    }

    if (b_cur.done != NULL) {
        b_cur.done(ok);
    }
    b_busy = false;
}

/*
 * Освободить шину и заново инициализировать блок I2C. Незавершённая
 * транзакция, если она была, завершается с ошибкой.
 */
static void i2c_bus_restart(void)
{
    /* Прерывания I2C запрещены: завершение не может прийти посреди перезапуска */
    i2c_bus_irq_enable(false);
    i2c_bus_stats.timeouts++;

    (void)HAL_I2C_DeInit(&b_hi2c);
    i2c_bus_hw_setup();

    if (b_busy) {
        if (b_cur.done != NULL) {
            b_cur.done(false);
        }
        b_busy = false;
    }

    if (b_async) {
        HAL_NVIC_ClearPendingIRQ(board_i2c_ev_irqn());
        HAL_NVIC_ClearPendingIRQ(board_i2c_er_irqn());
        i2c_bus_irq_enable(true);
    }
}

void i2c_bus_watchdog(uint32_t now_ms)
{
    if (!b_busy || ((uint32_t)(now_ms - b_start_ms) <= I2C_TXN_TIMEOUT_MS)) {
        return;
    }

    /* Транзакция могла завершиться только что: проверяем ещё раз без прерываний I2C */
    i2c_bus_irq_enable(false);
    if (b_busy) {
        i2c_bus_restart();
    } else if (b_async) {
        i2c_bus_irq_enable(true);
    }
}

void HAL_I2C_MemTxCpltCallback(I2C_HandleTypeDef *hi2c)
{
    if (hi2c == &b_hi2c) {
        i2c_bus_finish(true);
    }
}

void HAL_I2C_MemRxCpltCallback(I2C_HandleTypeDef *hi2c)
{
    if (hi2c == &b_hi2c) {
        i2c_bus_finish(true);
    }
}

void HAL_I2C_ErrorCallback(I2C_HandleTypeDef *hi2c)
{
    if (hi2c == &b_hi2c) {
        i2c_bus_finish(false);
    }
}

void BOARD_I2C_EV_IRQHandler(void)
{
    HAL_I2C_EV_IRQHandler(&b_hi2c);
}

void BOARD_I2C_ER_IRQHandler(void)
{
    HAL_I2C_ER_IRQHandler(&b_hi2c);
}
