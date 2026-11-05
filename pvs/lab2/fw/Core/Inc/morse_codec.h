#ifndef MORSE_CODEC_H
#define MORSE_CODEC_H

#include <stdint.h>
#include <stdbool.h>

#include "morse_types.h"

/*
 * Перекодировка между буквами латинского алфавита и последовательностями
 * точек и тире международной азбуки Морзе. Не зависит от аппаратуры.
 */

/* Самая длинная буква латиницы в азбуке Морзе - 4 элемента */
#define MORSE_LETTER_MAX_LEN  4u

/* Заглавная форма буквы латинского алфавита (в любом регистре); '\0' - c не такая буква */
char morse_letter(char c);

/*
 * Закодировать букву (регистр не важен): элементы записываются в out, их
 * число - в *len. false - символ не является буквой латинского алфавита.
 */
bool morse_encode(char c, morse_symbol_t out[MORSE_LETTER_MAX_LEN], uint8_t *len);

/* Заглавная латинская буква для последовательности; '\0' - такой буквы нет */
char morse_decode(const morse_symbol_t *seq, uint8_t len);

/* Запись последовательности точками и тире, например "-.-"; out - не короче len + 1 */
void morse_to_text(const morse_symbol_t *seq, uint8_t len, char *out);

#endif /* MORSE_CODEC_H */
