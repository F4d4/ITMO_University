#include "sched.h"
#include "sys_tick.h"

static sched_task_t *s_tasks;
static uint8_t       s_count;

void sched_init(sched_task_t *tasks, uint8_t count)
{
    const uint32_t now = sys_tick_ms();

    s_tasks = tasks;
    s_count = count;

    for (uint8_t i = 0u; i < count; ++i) {
        tasks[i].last_ms = now;
    }
}

void sched_run_once(void)
{
    const uint32_t now = sys_tick_ms();

    for (uint8_t i = 0u; i < s_count; ++i) {
        sched_task_t *t = &s_tasks[i];

        if (t->period_ms == 0u) {
            t->fn(t->ctx);
            continue;
        }

        if ((uint32_t)(now - t->last_ms) >= t->period_ms) {
            /*
             * Сдвиг на период, а не присваивание now: так не накапливается
             * систематическое отставание. Если проход задержался больше чем
             * на период, ресинхронизируемся, чтобы не догонять пачкой вызовов.
             */
            t->last_ms += t->period_ms;
            if ((uint32_t)(now - t->last_ms) > t->period_ms) {
                t->last_ms = now;
            }
            t->fn(t->ctx);
        }
    }
}

void sched_run_forever(void)
{
    for (;;) {
        sched_run_once();
    }
}
