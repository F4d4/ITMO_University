#include <stddef.h>

#include "morse_player.h"

static void morse_player_sink(morse_player_t *p, bool on)
{
    if (p->sink != NULL) {
        p->sink(on, p->ctx);
    }
}

static uint32_t morse_player_units(const morse_player_t *p, uint8_t units)
{
    return (uint32_t)units * (uint32_t)p->timing.unit_ms;
}

static void morse_player_enter(morse_player_t *p, morse_player_state_t state)
{
    uint8_t units = 0u;

    p->state = state;

    switch (state) {
    case MORSE_PLAYER_LEAD_IN:
        morse_player_sink(p, false);
        units = p->timing.lead_in_units;
        break;

    case MORSE_PLAYER_MARK:
        morse_player_sink(p, true);
        units = (p->seq[p->index] == MORSE_DASH) ? p->timing.dash_units
                                                 : p->timing.dot_units;
        break;

    case MORSE_PLAYER_GAP:
        morse_player_sink(p, false);
        units = p->timing.gap_units;
        break;

    case MORSE_PLAYER_TAIL:
        morse_player_sink(p, false);
        units = p->timing.tail_units;
        break;

    case MORSE_PLAYER_IDLE:
    default:
        morse_player_sink(p, false);
        swtimer_stop(&p->timer);
        return;
    }

    swtimer_start(&p->timer, morse_player_units(p, units));
}

void morse_player_init(morse_player_t *p, const morse_timing_t *timing,
                       morse_sink_fn sink, void *ctx)
{
    p->timing = *timing;
    p->sink   = sink;
    p->ctx    = ctx;
    p->len    = 0u;
    p->index  = 0u;
    p->state  = MORSE_PLAYER_IDLE;
    swtimer_stop(&p->timer);
}

bool morse_player_start(morse_player_t *p, const morse_symbol_t *seq, uint8_t len)
{
    if ((p->state != MORSE_PLAYER_IDLE) || (len == 0u) || (len > MORSE_CAPACITY)) {
        return false;
    }

    /*
     * Последовательность копируется: передатчик не зависит от времени жизни
     * буфера, поэтому прикладной автомат волен очистить его когда угодно.
     */
    for (uint8_t i = 0u; i < len; ++i) {
        p->seq[i] = seq[i];
    }
    p->len   = len;
    p->index = 0u;

    morse_player_enter(p, MORSE_PLAYER_LEAD_IN);
    return true;
}

bool morse_player_busy(const morse_player_t *p)
{
    return p->state != MORSE_PLAYER_IDLE;
}

void morse_player_abort(morse_player_t *p)
{
    morse_player_enter(p, MORSE_PLAYER_IDLE);
}

void morse_player_task(void *ctx)
{
    morse_player_t *p = (morse_player_t *)ctx;

    if (p->state == MORSE_PLAYER_IDLE) {
        return;
    }
    if (!swtimer_expired(&p->timer)) {
        return;
    }

    switch (p->state) {
    case MORSE_PLAYER_LEAD_IN:
        morse_player_enter(p, MORSE_PLAYER_MARK);
        break;

    case MORSE_PLAYER_MARK:
        p->index++;
        /* После последнего элемента межэлементный интервал не нужен */
        morse_player_enter(p, (p->index < p->len) ? MORSE_PLAYER_GAP
                                                  : MORSE_PLAYER_TAIL);
        break;

    case MORSE_PLAYER_GAP:
        morse_player_enter(p, MORSE_PLAYER_MARK);
        break;

    case MORSE_PLAYER_TAIL:
    default:
        morse_player_enter(p, MORSE_PLAYER_IDLE);
        break;
    }
}
