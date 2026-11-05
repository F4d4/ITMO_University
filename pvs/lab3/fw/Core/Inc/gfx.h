#ifndef GFX_H
#define GFX_H

#include <stdbool.h>
#include <stdint.h>

/*
 * Графические примитивы в буфере кадра 128 x 64 в формате видеопамяти
 * SSD1306 (8 страниц по 128 байт, младший бит байта - верхняя точка).
 * Начало координат - левый верхний угол, ось y направлена вниз.
 * Точки за пределами экрана отбрасываются.
 */

#define GFX_WIDTH    128
#define GFX_HEIGHT    64

/* Знакоместо шрифта: символ 5 x 7 и промежутки по одной точке */
#define GFX_CHAR_W     6
#define GFX_CHAR_H     8

void           gfx_clear(void);
void           gfx_pixel(int x, int y);
void           gfx_line(int x0, int y0, int x1, int y1);
void           gfx_circle(int cx, int cy, int r);
void           gfx_fill_circle(int cx, int cy, int r);

/* Строка шрифтом 5 x 7; (x, y) - левый верхний угол первого знакоместа */
void           gfx_text(int x, int y, const char *s);

const uint8_t *gfx_frame(void);

#endif /* GFX_H */
