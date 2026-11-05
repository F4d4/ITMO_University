#include <stdlib.h>
#include <string.h>

#include "gfx.h"
#include "font5x7.h"

static uint8_t b_fb[GFX_WIDTH * GFX_HEIGHT / 8];

void gfx_clear(void)
{
    memset(b_fb, 0, sizeof(b_fb));
}

void gfx_pixel(int x, int y)
{
    if ((x < 0) || (x >= GFX_WIDTH) || (y < 0) || (y >= GFX_HEIGHT)) {
        return;
    }
    b_fb[(y / 8) * GFX_WIDTH + x] |= (uint8_t)(1u << (y % 8));
}

/* Алгоритм Брезенхема */
void gfx_line(int x0, int y0, int x1, int y1)
{
    const int dx  = abs(x1 - x0);
    const int dy  = -abs(y1 - y0);
    const int sx  = (x0 < x1) ? 1 : -1;
    const int sy  = (y0 < y1) ? 1 : -1;
    int       err = dx + dy;

    for (;;) {
        gfx_pixel(x0, y0);
        if ((x0 == x1) && (y0 == y1)) {
            break;
        }

        const int e2 = 2 * err;

        if (e2 >= dy) {
            err += dy;
            x0  += sx;
        }
        if (e2 <= dx) {
            err += dx;
            y0  += sy;
        }
    }
}

/* Алгоритм средней точки: строится одна восьмая, остальное - симметрией */
void gfx_circle(int cx, int cy, int r)
{
    int x   = r;
    int y   = 0;
    int err = 1 - r;

    while (x >= y) {
        gfx_pixel(cx + x, cy + y);
        gfx_pixel(cx + y, cy + x);
        gfx_pixel(cx - y, cy + x);
        gfx_pixel(cx - x, cy + y);
        gfx_pixel(cx - x, cy - y);
        gfx_pixel(cx - y, cy - x);
        gfx_pixel(cx + y, cy - x);
        gfx_pixel(cx + x, cy - y);

        y++;
        if (err < 0) {
            err += 2 * y + 1;
        } else {
            x--;
            err += 2 * (y - x) + 1;
        }
    }
}

void gfx_fill_circle(int cx, int cy, int r)
{
    for (int dy = -r; dy <= r; ++dy) {
        for (int dx = -r; dx <= r; ++dx) {
            if (dx * dx + dy * dy <= r * r) {
                gfx_pixel(cx + dx, cy + dy);
            }
        }
    }
}

static void gfx_char(int x, int y, char c)
{
    const int idx = (int)(unsigned char)c - FONT5X7_FIRST;

    if ((idx < 0) || (idx >= FONT5X7_COUNT)) {
        return;
    }

    for (int row = 0; row < FONT5X7_ROWS; ++row) {
        const uint8_t bits = font5x7[idx][row];

        for (int col = 0; col < 5; ++col) {
            if ((bits & (0x10u >> col)) != 0u) {
                gfx_pixel(x + col, y + row);
            }
        }
    }
}

void gfx_text(int x, int y, const char *s)
{
    for (; *s != '\0'; ++s) {
        gfx_char(x, y, *s);
        x += GFX_CHAR_W;
    }
}

const uint8_t *gfx_frame(void)
{
    return b_fb;
}
