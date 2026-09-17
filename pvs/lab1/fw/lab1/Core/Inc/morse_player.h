#ifndef MORSE_PLAYER_H
#define MORSE_PLAYER_H

#include <stdint.h>
#include <stdbool.h>

#include "morse_types.h"
#include "app_config.h"
#include "swtimer.h"

/*
 * Неблокирующий передатчик последовательности азбуки Морзе.
 *
 * Передатчик не знает, чем именно выдаётся сигнал: он лишь включает и
 * выключает абстрактный выход. В этой работе выходом служит зелёный
 * светодиод, в следующих к тому же передатчику без изменений можно
 * подключить звукоизлучатель или последовательный канал.
 */

typedef struct {
    uint16_t unit_ms;        /* базовая единица T (длительность точки) */
    uint8_t  dot_units;
    uint8_t  dash_units;
    uint8_t  gap_units;      /* межэлементный интервал */
    uint8_t  lead_in_units;  /* пауза перед началом передачи */
    uint8_t  tail_units;     /* пауза после передачи */
} morse_timing_t;

typedef void (*morse_sink_fn)(bool on, void *ctx);

typedef enum {
    MORSE_PLAYER_IDLE = 0,
    MORSE_PLAYER_LEAD_IN,
    MORSE_PLAYER_MARK,
    MORSE_PLAYER_GAP,
    MORSE_PLAYER_TAIL
} morse_player_state_t;

typedef struct {
    morse_timing_t       timing;
    morse_sink_fn        sink;
    void                *ctx;
    morse_symbol_t       seq[MORSE_CAPACITY];
    uint8_t              len;
    uint8_t              index;
    morse_player_state_t state;
    swtimer_t            timer;
} morse_player_t;

void morse_player_init (morse_player_t *p, const morse_timing_t *timing,
                        morse_sink_fn sink, void *ctx);

/* false - передатчик занят или последовательность пуста либо слишком длинна */
bool morse_player_start(morse_player_t *p, const morse_symbol_t *seq, uint8_t len);

bool morse_player_busy (const morse_player_t *p);
void morse_player_abort(morse_player_t *p);

/* Шаг передатчика; управление возвращается немедленно */
void morse_player_task (void *p);

#endif /* MORSE_PLAYER_H */
