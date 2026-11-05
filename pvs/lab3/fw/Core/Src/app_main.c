#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

#include "app_main.h"
#include "accel.h"
#include "app_config.h"
#include "board.h"
#include "console.h"
#include "gfx.h"
#include "i2c_bus.h"
#include "i2c_sched.h"
#include "keypad.h"
#include "led.h"
#include "level.h"
#include "oled.h"
#include "proto.h"
#include "serial.h"
#include "sys_tick.h"

/*
 * Верхний уровень программы "Уровень".
 *
 * Обмен по I2C идёт в прерываниях по расписанию (i2c_sched); главный цикл
 * только забирает результаты и отдаёт новые кадры:
 *  - команды с ПК и нажатия клавиатуры меняют тип указателя;
 *  - как только дисплей принял предыдущий кадр, рисуется следующий;
 *  - каждые ACC_REPORT_PERIOD_MS показания и тип указателя уходят на ПК.
 */

/* Кнопки клавиатуры, выбирающие тип указателя */
#define KEY_MODE_HORIZON  1u
#define KEY_MODE_ARROW    2u

static level_mode_t b_mode;
static uint8_t      b_last_key;
static bool         b_oled_ok;
static uint32_t     b_report_last_ms;

/*
 * true, если с момента *last_ms прошёл период. Момент сдвигается на период, а
 * не присваивается now: так не накапливается отставание. Если проход
 * задержался больше чем на период, момент переносится на now, чтобы не
 * догонять пропуски пачкой вызовов.
 */
static bool app_period_elapsed(uint32_t *last_ms, uint32_t period_ms, uint32_t now)
{
    if ((uint32_t)(now - *last_ms) < period_ms) {
        return false;
    }

    *last_ms += period_ms;
    if ((uint32_t)(now - *last_ms) > period_ms) {
        *last_ms = now;
    }
    return true;
}

static void app_handle_command(proto_cmd_t cmd)
{
    switch (cmd) {
    case PROTO_CMD_PING:
        proto_send_pong();
        break;
    case PROTO_CMD_MODE_HORIZON:
        b_mode = LEVEL_MODE_HORIZON;
        proto_send_mode_ok(b_mode);
        break;
    case PROTO_CMD_MODE_ARROW:
        b_mode = LEVEL_MODE_ARROW;
        proto_send_mode_ok(b_mode);
        break;
    case PROTO_CMD_INVALID:
        proto_send_error();
        break;
    case PROTO_CMD_NONE:
    default:
        break;
    }
}

static void app_handle_key(uint8_t code)
{
    b_last_key = code;
    proto_send_key(code);

    if (code == KEY_MODE_HORIZON) {
        b_mode = LEVEL_MODE_HORIZON;
    } else if (code == KEY_MODE_ARROW) {
        b_mode = LEVEL_MODE_ARROW;
    }
}

static void app_current_view(level_view_t *v, uint32_t now)
{
    accel_sample_t s;
    const bool     have  = accel_get(&s);
    const bool     fresh = have && ((uint32_t)(now - s.t_ms) <= ACC_STALE_MS);

    level_compute(&s, fresh, v);
}

void app_setup(void)
{
    board_init();
    led_init();
    serial_init(SERIAL_START_MODE);

    /* Начальная инициализация устройств - по опросу, прерывания I2C ещё запрещены */
    i2c_bus_init();
    b_oled_ok = oled_init();
    const bool accel_ok  = accel_init();
    const bool keypad_ok = keypad_init();

    if (!b_oled_ok) {
        (void)console_message("ERR INIT OLED");
    }
    if (!accel_ok) {
        (void)console_message("ERR INIT ACCEL");
    }
    if (!keypad_ok) {
        (void)console_message("ERR INIT KEYPAD");
    }

    b_mode           = LEVEL_MODE_HORIZON;
    b_last_key       = 0u;
    b_report_last_ms = sys_tick_ms();

    i2c_bus_start_async();
    i2c_sched_start();
}

void app_step(void)
{
    serial_task();
    app_handle_command(proto_poll());

    uint8_t code;

    while (keypad_pop(&code)) {
        app_handle_key(code);
    }

    const uint32_t now    = sys_tick_ms();
    const bool     render = b_oled_ok && oled_ready();
    const bool     report = app_period_elapsed(&b_report_last_ms, ACC_REPORT_PERIOD_MS, now);

    if (!render && !report) {
        return;
    }

    level_view_t v;

    app_current_view(&v, now);

    if (render) {
        level_render(&v, b_mode, b_last_key);
        (void)oled_submit(gfx_frame());
    }
    if (report) {
        proto_send_acc(&v, b_mode);
    }
}

void app_main(void)
{
    app_setup();

    for (;;) {
        app_step();
    }
}
