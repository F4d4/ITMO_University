#include "sim_time.h"
#include "sys_tick.h"

static uint32_t b_now_ms;

void sim_time_reset(void)
{
    b_now_ms = 0u;
}

uint32_t sim_time_now(void)
{
    return b_now_ms;
}

void sim_time_advance(uint32_t ms)
{
    b_now_ms += ms;
}

uint32_t sys_tick_ms(void)
{
    return b_now_ms;
}
