#include <math.h>
#include <stdio.h>

#include "level.h"
#include "app_config.h"
#include "board_pins.h"
#include "gfx.h"

#define PI_F  3.14159265f

/* Шкала указателя - окружность в правой половине экрана */
#define DIAL_CX         96
#define DIAL_CY         32
#define DIAL_R          30
#define ARROW_HEAD_LEN   8
#define ARROW_HEAD_DEG  25

/* Текст - в левой половине */
#define TEXT_X           0
#define TEXT_MODE_Y      0
#define TEXT_AXIS_Y     16
#define TEXT_AXIS_STEP  12
#define TEXT_KEY_Y      56

char level_mode_letter(level_mode_t mode)
{
    return (mode == LEVEL_MODE_ARROW) ? 'A' : 'H';
}

void level_format_g(char *buf, int16_t mg)
{
    const int  mag   = (mg < 0) ? -(int)mg : (int)mg;
    const int  centi = (mag + 5) / 10;
    /* После округления до нуля знак не показываем минусом: "-0.00" мигал бы с "+0.00" */
    const char sign  = ((mg < 0) && (centi != 0)) ? '-' : '+';

    (void)sprintf(buf, "%c%d.%02d", sign, centi / 100, centi % 100);
}

void level_compute(const accel_sample_t *s, bool valid, level_view_t *v)
{
    v->valid     = valid;
    v->flat      = true;
    v->angle_deg = 0;
    if (!valid) {
        return;
    }

    for (uint32_t i = 0u; i < 3u; ++i) {
        v->mg[i] = s->mg[i];
    }

    /*
     * В покое акселерометр показывает +1g по оси, направленной вверх, так
     * что сила тяжести направлена против показаний. Её проекция на оси
     * экрана (x - вправо, y - вниз):
     */
    const float gx = -(float)(BOARD_ACCEL_RIGHT_SIGN * s->mg[BOARD_ACCEL_RIGHT_AXIS]);
    const float gy = -(float)(BOARD_ACCEL_DOWN_SIGN  * s->mg[BOARD_ACCEL_DOWN_AXIS]);

    if (hypotf(gx, gy) < (float)LEVEL_FLAT_MG) {
        return;
    }

    int deg = (int)lroundf(atan2f(gy, gx) * 180.0f / PI_F);

    if (deg < 0) {
        deg += 360;
    }
    v->flat      = false;
    v->angle_deg = (int16_t)(deg % 360);
}

static int level_round(float f)
{
    return (int)lroundf(f);
}

static void level_draw_pointer(const level_view_t *v, level_mode_t mode)
{
    gfx_circle(DIAL_CX, DIAL_CY, DIAL_R);

    if (!v->valid) {
        return;
    }
    if (v->flat) {
        /* Сила тяжести перпендикулярна экрану: вместо указателя - точка в центре */
        gfx_fill_circle(DIAL_CX, DIAL_CY, 3);
        return;
    }

    const float a  = (float)v->angle_deg * PI_F / 180.0f;
    const float ux = cosf(a);
    const float uy = sinf(a);

    if (mode == LEVEL_MODE_ARROW) {
        /* Стрелка из центра по направлению силы тяжести */
        const float tip = (float)(DIAL_R - 3);
        const int   tx  = DIAL_CX + level_round(ux * tip);
        const int   ty  = DIAL_CY + level_round(uy * tip);
        const float h   = (float)ARROW_HEAD_DEG * PI_F / 180.0f;

        gfx_fill_circle(DIAL_CX, DIAL_CY, 1);
        gfx_line(DIAL_CX, DIAL_CY, tx, ty);
        gfx_line(tx, ty, tx - level_round(cosf(a - h) * ARROW_HEAD_LEN),
                         ty - level_round(sinf(a - h) * ARROW_HEAD_LEN));
        gfx_line(tx, ty, tx - level_round(cosf(a + h) * ARROW_HEAD_LEN),
                         ty - level_round(sinf(a + h) * ARROW_HEAD_LEN));
    } else {
        /* Горизонт - диаметр шкалы, перпендикулярный силе тяжести */
        const float r  = (float)(DIAL_R - 1);
        const int   hx = level_round(-uy * r);
        const int   hy = level_round(ux * r);

        gfx_line(DIAL_CX - hx, DIAL_CY - hy, DIAL_CX + hx, DIAL_CY + hy);
    }
}

void level_render(const level_view_t *v, level_mode_t mode, uint8_t last_key)
{
    static const char k_axis[3] = { 'X', 'Y', 'Z' };
    char line[16];

    gfx_clear();

    gfx_text(TEXT_X, TEXT_MODE_Y, (mode == LEVEL_MODE_ARROW) ? "ARROW" : "HORIZON");

    for (uint32_t i = 0u; i < 3u; ++i) {
        const int y = TEXT_AXIS_Y + (int)i * TEXT_AXIS_STEP;

        if (v->valid) {
            char g[8];

            level_format_g(g, v->mg[i]);
            (void)snprintf(line, sizeof(line), "%c:%sg", k_axis[i], g);
        } else {
            (void)snprintf(line, sizeof(line), "%c: ---", k_axis[i]);
        }
        gfx_text(TEXT_X, y, line);
    }

    if (!v->valid) {
        gfx_text(TEXT_X, TEXT_KEY_Y, "NO SENSOR");
    } else if (last_key != 0u) {
        (void)snprintf(line, sizeof(line), "KEY:%u", (unsigned)last_key);
        gfx_text(TEXT_X, TEXT_KEY_Y, line);
    }

    level_draw_pointer(v, mode);
}
