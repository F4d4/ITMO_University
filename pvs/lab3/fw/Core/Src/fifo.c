#include <stdatomic.h>

#include "fifo.h"

/*
 * Барьеры компилятора. Массив buf не объявлен volatile, поэтому без барьера
 * компилятор вправе переставить запись байта в массив после сдвига head (или
 * чтение байта - после сдвига tail), и прервавшая сторона увидела бы индекс
 * раньше данных. atomic_signal_fence запрещает такую перестановку, не
 * порождая лишних команд: процессор Cortex-M4 одноядерный, без кэша данных,
 * и сам выполняет обращения к памяти в программном порядке.
 */
#define FIFO_RELEASE()  atomic_signal_fence(memory_order_release)
#define FIFO_ACQUIRE()  atomic_signal_fence(memory_order_acquire)

static uint16_t fifo_next(const fifo_t *f, uint16_t index, uint16_t step)
{
    const uint32_t next = (uint32_t)index + step;

    return (uint16_t)((next >= f->size) ? (next - f->size) : next);
}

static uint16_t fifo_used(const fifo_t *f, uint16_t head, uint16_t tail)
{
    return (uint16_t)((head >= tail) ? (head - tail) : (f->size - tail + head));
}

void fifo_init(fifo_t *f, uint8_t *storage, uint16_t storage_size)
{
    f->buf  = storage;
    f->size = storage_size;
    f->head = 0u;
    f->tail = 0u;
}

uint16_t fifo_count(const fifo_t *f)
{
    return fifo_used(f, f->head, f->tail);
}

uint16_t fifo_capacity(const fifo_t *f)
{
    return (uint16_t)(f->size - 1u);
}

uint16_t fifo_free(const fifo_t *f)
{
    return (uint16_t)(fifo_capacity(f) - fifo_count(f));
}

bool fifo_put(fifo_t *f, uint8_t byte)
{
    const uint16_t head = f->head;
    const uint16_t next = fifo_next(f, head, 1u);

    if (next == f->tail) {
        return false;
    }

    f->buf[head] = byte;
    FIFO_RELEASE();
    f->head = next;
    return true;
}

bool fifo_write(fifo_t *f, const uint8_t *data, uint16_t len)
{
    if (len > fifo_free(f)) {
        return false;
    }

    uint16_t head = f->head;

    for (uint16_t i = 0u; i < len; ++i) {
        f->buf[head] = data[i];
        head = fifo_next(f, head, 1u);
    }

    /* Читатель увидит всё сообщение разом, а не по частям */
    FIFO_RELEASE();
    f->head = head;
    return true;
}

bool fifo_get(fifo_t *f, uint8_t *byte)
{
    const uint16_t tail = f->tail;

    if (tail == f->head) {
        return false;
    }
    FIFO_ACQUIRE();

    *byte = f->buf[tail];
    FIFO_RELEASE();
    f->tail = fifo_next(f, tail, 1u);
    return true;
}

uint16_t fifo_peek_linear(const fifo_t *f, const uint8_t **data)
{
    const uint16_t head = f->head;
    const uint16_t tail = f->tail;

    FIFO_ACQUIRE();
    *data = &f->buf[tail];

    /* Данные, перешедшие через конец массива, достанутся следующему вызову */
    return (uint16_t)((head >= tail) ? (head - tail) : (f->size - tail));
}

void fifo_drop(fifo_t *f, uint16_t n)
{
    const uint16_t tail = f->tail;

    FIFO_RELEASE();
    f->tail = fifo_next(f, tail, n);
}

void fifo_flush(fifo_t *f)
{
    f->tail = f->head;
}
