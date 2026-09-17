#ifndef SCHED_H
#define SCHED_H

#include <stdint.h>

/*
 * Кооперативный планировщик. Псевдопараллельность без ОСРВ и без прерываний:
 * задача выполняет один короткий шаг и возвращает управление, поэтому
 * ни одна задача не блокирует остальные.
 */
typedef void (*sched_task_fn)(void *ctx);

typedef struct {
    sched_task_fn fn;
    void         *ctx;
    uint32_t      period_ms;  /* 0 - вызывать на каждом проходе */
    uint32_t      last_ms;    /* служебное поле планировщика */
} sched_task_t;

void sched_init       (sched_task_t *tasks, uint8_t count);
void sched_run_once   (void);   /* один проход по таблице задач */
void sched_run_forever(void);

#endif /* SCHED_H */
