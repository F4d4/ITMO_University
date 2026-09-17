#include "sim_stimulus.h"

static const stim_event_t *b_events;
static size_t              b_count;
static size_t              b_next;

void stim_load(const stim_event_t *events, size_t count)
{
    b_events = events;
    b_count  = count;
    b_next   = 0u;
}

void stim_apply_until(uint32_t t_ms)
{
    while ((b_next < b_count) && (b_events[b_next].t_ms <= t_ms)) {
        const stim_event_t *e = &b_events[b_next];

        gpio_sim_set_pad(e->port, e->pin, e->drive, e->note);
        b_next++;
    }
}

size_t stim_pending(void)
{
    return b_count - b_next;
}
