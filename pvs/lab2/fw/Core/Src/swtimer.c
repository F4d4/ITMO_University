#include "swtimer.h"
#include "sys_tick.h"

void swtimer_start(swtimer_t *t, uint32_t period_ms)
{
    t->start_ms  = sys_tick_ms();
    t->period_ms = period_ms;
    t->active    = true;
}

void swtimer_stop(swtimer_t *t)
{
    t->active = false;
}

bool swtimer_active(const swtimer_t *t)
{
    return t->active;
}

uint32_t swtimer_elapsed(const swtimer_t *t)
{
    /* Разность беззнаковых остаётся верной при обороте 32-битного счётчика */
    return (uint32_t)(sys_tick_ms() - t->start_ms);
}

bool swtimer_expired(swtimer_t *t)
{
    if (!t->active) {
        return false;
    }
    if ((uint32_t)(sys_tick_ms() - t->start_ms) < t->period_ms) {
        return false;
    }
    t->active = false;
    return true;
}
