#include "morse_buffer.h"

void morse_buffer_clear(morse_buffer_t *b)
{
    b->len = 0u;
}

bool morse_buffer_push(morse_buffer_t *b, morse_symbol_t s)
{
    if (b->len >= MORSE_CAPACITY) {
        return false;
    }
    b->item[b->len] = s;
    b->len++;
    return true;
}

uint8_t morse_buffer_len(const morse_buffer_t *b)
{
    return b->len;
}

bool morse_buffer_full(const morse_buffer_t *b)
{
    return b->len >= MORSE_CAPACITY;
}

const morse_symbol_t *morse_buffer_data(const morse_buffer_t *b)
{
    return b->item;
}
