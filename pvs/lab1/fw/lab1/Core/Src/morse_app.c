#include <stdbool.h>
#include <stddef.h>

#include "morse_app.h"
#include "morse_buffer.h"
#include "morse_player.h"
#include "app_config.h"
#include "button.h"
#include "led.h"
#include "swtimer.h"

/*
 * Вариант 3, "Азбука Морзе".
 *
 * Короткое нажатие кнопки - точка, длинное - тире; запоминается до восьми
 * нажатий. После каждого нажатия двухцветный светодиод коротко показывает
 * введённый сигнал: точка жёлтым, тире красным. Долгая пауза означает конец
 * ввода: последовательность один раз проигрывается зелёным светодиодом
 * короткими и длинными импульсами, после чего программа снова готова
 * принимать ввод.
 *
 * В этом файле нет ни одного обращения к аппаратуре: ни номеров выводов, ни
 * вызовов HAL. Всё взаимодействие идёт через драйверы кнопки и светодиодов.
 */

typedef enum {
    APP_IDLE = 0,   /* буфер пуст, ждём первое нажатие   */
    APP_INPUT,      /* идёт ввод последовательности       */
    APP_SENDING     /* последовательность проигрывается   */
} morse_app_state_t;

static morse_app_state_t b_state;
static morse_buffer_t    b_buffer;
static morse_player_t    b_player;
static swtimer_t         b_idle;
static uint32_t          b_press_ms;
static bool              b_press_valid;

static const morse_timing_t b_timing = {
    .unit_ms       = MORSE_UNIT_MS,
    .dot_units     = MORSE_DOT_UNITS,
    .dash_units    = MORSE_DASH_UNITS,
    .gap_units     = MORSE_GAP_UNITS,
    .lead_in_units = MORSE_LEAD_IN_UNITS,
    .tail_units    = MORSE_TAIL_UNITS
};

/* Абстрактный выход передатчика подключён к зелёному светодиоду */
static void morse_app_sink(bool on, void *ctx)
{
    (void)ctx;
    led_set(LED_CH_GREEN, on ? LED_ON : LED_OFF);
}

static led_color_t morse_app_feedback_color(morse_symbol_t s)
{
    return (s == MORSE_DOT) ? LED_YELLOW : LED_RED;
}

static void morse_app_begin_sending(void)
{
    /* Индикация ввода гасится, чтобы не мешать восприятию передачи */
    led_set(LED_CH_BICOLOR, LED_OFF);

    if (morse_player_start(&b_player,
                           morse_buffer_data(&b_buffer),
                           morse_buffer_len(&b_buffer))) {
        b_state = APP_SENDING;
    } else {
        morse_buffer_clear(&b_buffer);
        b_state = APP_IDLE;
    }
}

static void morse_app_on_press(uint32_t t_ms)
{
    b_press_ms    = t_ms;
    b_press_valid = true;

    /*
     * Отсчёт паузы прерывается на время удержания кнопки: иначе нажатие
     * длиннее тайм-аута запустило бы передачу прямо посреди ввода символа.
     */
    swtimer_stop(&b_idle);
}

static void morse_app_on_release(uint32_t t_ms)
{
    if (!b_press_valid) {
        return;
    }
    b_press_valid = false;

    const uint32_t duration = (uint32_t)(t_ms - b_press_ms);
    const morse_symbol_t symbol = (duration < MORSE_DASH_THRESHOLD_MS) ? MORSE_DOT
                                                                       : MORSE_DASH;

    if (morse_buffer_full(&b_buffer)) {
        /* Лишние нажатия не запоминаются, о чём сообщает частое мигание красным */
        led_blink(LED_CH_BICOLOR, LED_RED,
                  MORSE_ERR_STEP_MS, MORSE_ERR_STEP_MS, MORSE_ERR_CYCLES);
    } else {
        (void)morse_buffer_push(&b_buffer, symbol);
        /*
         * Сигнал показывается на отпускании: до него ещё неизвестно, окажется
         * нажатие точкой или тире.
         */
        led_pulse(LED_CH_BICOLOR, morse_app_feedback_color(symbol), MORSE_FEEDBACK_MS);
    }

    b_state = APP_INPUT;

#if MORSE_AUTOSEND_ON_FULL
    if (morse_buffer_full(&b_buffer)) {
        morse_app_begin_sending();
        return;
    }
#endif

    swtimer_start(&b_idle, MORSE_INPUT_TIMEOUT_MS);
}

void morse_app_init(void)
{
    b_state       = APP_IDLE;
    b_press_valid = false;
    b_press_ms    = 0u;

    morse_buffer_clear(&b_buffer);
    morse_player_init(&b_player, &b_timing, morse_app_sink, NULL);
    swtimer_stop(&b_idle);
}

void morse_app_task(void *ctx)
{
    (void)ctx;

    btn_event_t event;

    while (button_pop(&event)) {
        if (b_state == APP_SENDING) {
            /* Во время передачи ввод не принимается */
            continue;
        }
        if (event.type == BTN_EV_PRESSED) {
            morse_app_on_press(event.t_ms);
        } else {
            morse_app_on_release(event.t_ms);
        }
    }

    if ((b_state == APP_INPUT) && swtimer_expired(&b_idle)) {
        if (morse_buffer_len(&b_buffer) > 0u) {
            morse_app_begin_sending();
        } else {
            b_state = APP_IDLE;
        }
    }

    if (b_state == APP_SENDING) {
        morse_player_task(&b_player);

        if (!morse_player_busy(&b_player)) {
            morse_buffer_clear(&b_buffer);
            /*
             * Нажатия, сделанные во время передачи, не должны стать первыми
             * символами новой последовательности.
             */
            button_flush();
            b_press_valid = false;
            b_state       = APP_IDLE;
        }
    }
}
