#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

#include "app_main.h"
#include "app_config.h"
#include "board.h"
#include "button.h"
#include "led.h"
#include "morse_app.h"
#include "sys_tick.h"

/*
 * Верхний уровень программы.
 *
 * Здесь нет ни номеров выводов, ни обращений к регистрам, ни пауз с активным
 * ожиданием. Три независимых процесса выполняются псевдопараллельно в
 * бесконечном цикле: каждый делает короткий шаг и сразу возвращает управление.
 */

static uint32_t b_button_last_ms;
static uint32_t b_led_last_ms;

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

void app_setup(void)
{
    board_init();
    led_init();
    button_init();
    morse_app_init();

    b_button_last_ms = sys_tick_ms();
    b_led_last_ms    = b_button_last_ms;
}

void app_step(void)
{
    const uint32_t now = sys_tick_ms();

    if (app_period_elapsed(&b_button_last_ms, BTN_POLL_MS, now)) {
        button_task(NULL);      /* опрос кнопки и антидребезг */
    }
    if (app_period_elapsed(&b_led_last_ms, 1u, now)) {
        led_task(NULL);         /* выдержки времени индикации */
    }
    morse_app_task(NULL);       /* логика варианта задания, на каждом проходе */
}

void app_main(void)
{
    app_setup();

    for (;;) {
        app_step();
    }
}
