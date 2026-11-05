#ifndef SWTIMER_H
#define SWTIMER_H

#include <stdint.h>
#include <stdbool.h>

/*
 * Программный неблокирующий таймер. Активного ожидания нет:
 * вызывающий периодически спрашивает, истёк ли интервал.
 */
typedef struct {
    uint32_t start_ms;
    uint32_t period_ms;
    bool     active;
} swtimer_t;

void     swtimer_start  (swtimer_t *t, uint32_t period_ms);
void     swtimer_stop   (swtimer_t *t);
bool     swtimer_active (const swtimer_t *t);
uint32_t swtimer_elapsed(const swtimer_t *t);

/* true ровно один раз по истечении интервала; таймер при этом останавливается */
bool     swtimer_expired(swtimer_t *t);

#endif /* SWTIMER_H */
