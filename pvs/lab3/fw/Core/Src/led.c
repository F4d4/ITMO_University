#include "led.h"
#include "board.h"
#include "swtimer.h"

/*
 * Анимация каждого канала - самостоятельный конечный автомат. Ожидание
 * выдержки не блокирующее: led_task() лишь проверяет программный таймер и
 * сразу возвращает управление планировщику.
 */
typedef struct {
    led_color_t color[2];
    uint32_t    ms[2];
    uint16_t    cycles_left;   /* 0 - бесконечно */
    uint8_t     phase;
    bool        running;
    swtimer_t   timer;
    led_color_t current;
} led_channel_t;

static led_channel_t b_channel[LED_CH_COUNT];

static void led_apply(led_ch_t ch, led_color_t color)
{
    b_channel[ch].current = color;
    board_led_apply(ch, color);
}

/*
 * Вход в фазу. Фаза нулевой длительности не ждёт ничего: цвет применяется и
 * автомат сразу переходит дальше, поэтому led_pulse (вторая фаза длиной 0)
 * просто оставляет канал выключенным.
 */
static void led_enter_phase(led_ch_t ch, uint8_t phase)
{
    led_channel_t *c = &b_channel[ch];

    c->phase = phase;
    led_apply(ch, c->color[phase]);

    if (c->ms[phase] == 0u) {
        swtimer_stop(&c->timer);
    } else {
        swtimer_start(&c->timer, c->ms[phase]);
    }
}

static void led_advance(led_ch_t ch)
{
    led_channel_t *c = &b_channel[ch];

    if (c->phase == 0u) {
        led_enter_phase(ch, 1u);
        return;
    }

    /* Цикл завершён */
    if (c->cycles_left != 0u) {
        c->cycles_left--;
        if (c->cycles_left == 0u) {
            c->running = false;
            swtimer_stop(&c->timer);
            return;
        }
    }
    led_enter_phase(ch, 0u);
}

void led_init(void)
{
    for (uint8_t ch = 0u; ch < LED_CH_COUNT; ++ch) {
        b_channel[ch].running     = false;
        b_channel[ch].cycles_left = 0u;
        b_channel[ch].phase       = 0u;
        swtimer_stop(&b_channel[ch].timer);
        led_apply((led_ch_t)ch, LED_OFF);
    }
}

void led_set(led_ch_t ch, led_color_t color)
{
    if (ch >= LED_CH_COUNT) {
        return;
    }

    b_channel[ch].running = false;
    swtimer_stop(&b_channel[ch].timer);
    led_apply(ch, color);
}

led_color_t led_get(led_ch_t ch)
{
    return (ch < LED_CH_COUNT) ? b_channel[ch].current : LED_OFF;
}

void led_anim(led_ch_t ch,
              led_color_t a, uint32_t a_ms,
              led_color_t b, uint32_t b_ms,
              uint16_t cycles)
{
    if (ch >= LED_CH_COUNT) {
        return;
    }

    led_channel_t *c = &b_channel[ch];

    c->color[0]    = a;
    c->color[1]    = b;
    c->ms[0]       = a_ms;
    c->ms[1]       = b_ms;
    c->cycles_left = cycles;
    c->running     = true;

    led_enter_phase(ch, 0u);

    /*
     * Обе фазы нулевой длительности означали бы бесконечный холостой цикл,
     * поэтому такая анимация сразу завершается.
     */
    if ((a_ms == 0u) && (b_ms == 0u)) {
        c->running = false;
    }
}

bool led_busy(led_ch_t ch)
{
    return (ch < LED_CH_COUNT) && b_channel[ch].running;
}

void led_task(void *ctx)
{
    (void)ctx;

    for (uint8_t ch = 0u; ch < LED_CH_COUNT; ++ch) {
        led_channel_t *c = &b_channel[ch];

        if (!c->running) {
            continue;
        }

        /* Фаза нулевой длительности: таймер не запущен, переходим немедленно */
        if (!swtimer_active(&c->timer) || swtimer_expired(&c->timer)) {
            led_advance((led_ch_t)ch);
        }
    }
}
