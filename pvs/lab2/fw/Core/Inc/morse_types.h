#ifndef MORSE_TYPES_H
#define MORSE_TYPES_H

#include <stdint.h>

/*
 * Общие типы азбуки Морзе. Вынесены отдельно, чтобы буфер, передатчик и
 * прикладной автомат не зависели друг от друга, а в следующей работе к ним
 * можно было добавить таблицу перекодировки в символы латинского алфавита.
 */

typedef enum {
    MORSE_DOT  = 0,
    MORSE_DASH = 1
} morse_symbol_t;

#endif /* MORSE_TYPES_H */
