#ifndef BUTTON_H
#define BUTTON_H

#include <stdint.h>
#include <stdbool.h>

/*
 * Универсальный драйвер кнопки с программной защитой от дребезга.
 *
 * Драйвер сообщает только физические события - "нажата" и "отпущена" - с
 * меткой времени. Он ничего не знает ни о коротких и длинных нажатиях, ни о
 * точках и тире: классификация относится к прикладной логике.
 *
 * Функции неблокирующие, ожидания нажатия внутри драйвера нет.
 */

typedef enum {
    BTN_EV_PRESSED = 0,
    BTN_EV_RELEASED
} btn_ev_type_t;

typedef struct {
    btn_ev_type_t type;
    uint32_t      t_ms;
} btn_event_t;

void button_init(void);

/* Опрос входа и шаг счётчика антидребезга; вызывается планировщиком */
void button_task(void *ctx);

/* Извлечь очередное событие; false - очередь пуста */
bool button_pop(btn_event_t *out);

/* Устойчивое состояние кнопки после подавления дребезга */
bool button_is_down(void);

/* Отбросить накопленные события */
void button_flush(void);

#endif /* BUTTON_H */
