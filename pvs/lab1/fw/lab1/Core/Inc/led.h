#ifndef LED_H
#define LED_H

#include <stdint.h>
#include <stdbool.h>

/*
 * Универсальный драйвер светодиодной индикации стенда.
 *
 * Драйвер оперирует логическими каналами и цветами, не зная ни номеров
 * выводов, ни прикладного смысла индикации. Он пригоден для любого варианта
 * задания: мигающий светофор, гирлянда, подтверждение ввода и т. п.
 *
 * Все функции неблокирующие: ни одна не содержит ожидания события и ни одной
 * паузы с активным ожиданием. Выдержки времени отрабатывает led_task(),
 * которую периодически вызывает кооперативный планировщик.
 */

typedef enum {
    LED_CH_GREEN = 0,   /* одноцветный канал */
    LED_CH_BICOLOR,     /* двухцветный канал */
    LED_CH_COUNT
} led_ch_t;

typedef uint8_t led_color_t;

#define LED_OFF          0u
#define LED_ON           1u   /* для одноцветного канала */
#define LED_YELLOW       1u   /* для двухцветного канала */
#define LED_RED          2u   /* для двухцветного канала */
#define LED_COLOR_COUNT  3u

void led_init(void);

/* Немедленно установить цвет; начатая анимация отменяется */
void        led_set(led_ch_t ch, led_color_t color);
led_color_t led_get(led_ch_t ch);

/*
 * Двухфазная анимация: цвет a держится a_ms, затем цвет b держится b_ms,
 * и так cycles раз (cycles = 0 - бесконечно). Фаза нулевой длительности
 * пропускается. По завершении канал остаётся в цвете b.
 */
void led_anim(led_ch_t ch,
              led_color_t a, uint32_t a_ms,
              led_color_t b, uint32_t b_ms,
              uint16_t cycles);

bool led_busy(led_ch_t ch);

/* Шаг драйвера; вызывается планировщиком */
void led_task(void *ctx);

/* Частные случаи единственной реализации - без дублирования кода */
#define led_pulse(ch, color, ms)            led_anim((ch), (color), (ms), LED_OFF, 0u, 1u)
#define led_blink(ch, color, on, off, n)    led_anim((ch), (color), (on), LED_OFF, (off), (n))

#endif /* LED_H */
