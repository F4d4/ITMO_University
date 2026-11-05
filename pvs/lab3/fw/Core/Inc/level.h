#ifndef LEVEL_H
#define LEVEL_H

#include <stdbool.h>
#include <stdint.h>

#include "accel.h"

/*
 * Прикладная часть "Уровень": из показаний акселерометра получается
 * направление силы тяжести в плоскости экрана, и по нему рисуется один из
 * двух указателей - стрелка вниз или линия горизонта.
 */

typedef enum {
    LEVEL_MODE_HORIZON = 0,   /* по умолчанию */
    LEVEL_MODE_ARROW
} level_mode_t;

typedef struct {
    bool    valid;       /* есть свежие показания датчика */
    int16_t mg[3];       /* ускорение по осям X, Y, Z датчика, mg */
    bool    flat;        /* стенд лежит почти горизонтально: направление не определено */
    int16_t angle_deg;   /* направление силы тяжести на экране, 0..359: 0 - вправо, 90 - вниз */
} level_view_t;

/* valid = false - показаний нет или они устарели */
void level_compute(const accel_sample_t *s, bool valid, level_view_t *v);

/* Нарисовать экран в буфере gfx; last_key = 0 - кнопки ещё не нажимали */
void level_render(const level_view_t *v, level_mode_t mode, uint8_t last_key);

/* Буква режима в протоколе: H - горизонт, A - стрелка */
char level_mode_letter(level_mode_t mode);

/* mg -> "+0.98": знак и сотые доли g, округление половины от нуля */
void level_format_g(char *buf, int16_t mg);

#endif /* LEVEL_H */
