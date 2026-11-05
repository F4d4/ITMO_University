#ifndef MORSE_BUFFER_H
#define MORSE_BUFFER_H

#include <stdint.h>
#include <stdbool.h>

#include "morse_types.h"
#include "app_config.h"

/*
 * Буфер вводимой последовательности фиксированной ёмкости.
 * Не зависит ни от аппаратуры, ни от способа ввода, поэтому компилируется
 * и проверяется на инструментальной машине без изменений.
 */
typedef struct {
    morse_symbol_t item[MORSE_CAPACITY];
    uint8_t        len;
} morse_buffer_t;

void                  morse_buffer_clear(morse_buffer_t *b);
bool                  morse_buffer_push (morse_buffer_t *b, morse_symbol_t s);
uint8_t               morse_buffer_len  (const morse_buffer_t *b);
bool                  morse_buffer_full (const morse_buffer_t *b);
const morse_symbol_t *morse_buffer_data (const morse_buffer_t *b);

#endif /* MORSE_BUFFER_H */
