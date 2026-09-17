#include "button.h"
#include "board.h"
#include "app_config.h"
#include "sys_tick.h"

#define BTN_QUEUE_SIZE  8u

/*
 * Защита от дребезга счётчиком с гистерезисом.
 *
 * При каждом опросе счётчик увеличивается, если контакт замкнут, и
 * уменьшается, если разомкнут. Переход в состояние "нажата" происходит,
 * когда счётчик достигает BTN_CNT_PRESS, обратный - когда падает до
 * BTN_CNT_RELEASE. Разница между порогами и есть гистерезис: любая помеха,
 * не способная сместить счётчик на эту величину, отбрасывается.
 *
 * Оба события задерживаются относительно физического фронта на одинаковое
 * время, поэтому измеренная прикладной логикой длительность нажатия не
 * искажается, и порог классификации точки и тире не смещается.
 */
static uint8_t     b_counter;
static bool        b_down;

static btn_event_t b_queue[BTN_QUEUE_SIZE];
static uint8_t     b_head;
static uint8_t     b_tail;

static void button_push(btn_ev_type_t type, uint32_t t_ms)
{
    const uint8_t next = (uint8_t)((b_head + 1u) % BTN_QUEUE_SIZE);

    if (next == b_tail) {
        /* Очередь полна: ранее зафиксированные события важнее, новое отбрасываем */
        return;
    }

    b_queue[b_head].type = type;
    b_queue[b_head].t_ms = t_ms;
    b_head = next;
}

void button_init(void)
{
    b_counter = 0u;
    b_down    = false;
    b_head    = 0u;
    b_tail    = 0u;
}

void button_task(void *ctx)
{
    (void)ctx;

    if (board_button_is_closed()) {
        if (b_counter < BTN_CNT_MAX) {
            b_counter++;
        }
    } else {
        if (b_counter > 0u) {
            b_counter--;
        }
    }

    if (!b_down && (b_counter >= BTN_CNT_PRESS)) {
        b_down = true;
        button_push(BTN_EV_PRESSED, sys_tick_ms());
    } else if (b_down && (b_counter <= BTN_CNT_RELEASE)) {
        b_down = false;
        button_push(BTN_EV_RELEASED, sys_tick_ms());
    }
}

bool button_pop(btn_event_t *out)
{
    if (b_tail == b_head) {
        return false;
    }

    *out   = b_queue[b_tail];
    b_tail = (uint8_t)((b_tail + 1u) % BTN_QUEUE_SIZE);
    return true;
}

bool button_is_down(void)
{
    return b_down;
}

void button_flush(void)
{
    b_tail = b_head;
}
