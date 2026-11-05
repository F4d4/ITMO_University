#include <string.h>

#include "morse_codec.h"

/* Международная азбука Морзе, буквы A..Z */
static const char *const b_codes[26] = {
    ".-",   "-...", "-.-.", "-..",  ".",    "..-.", "--.",  "....", "..",
    ".---", "-.-",  ".-..", "--",   "-.",   "---",  ".--.", "--.-", ".-.",
    "...",  "-",    "..-",  "...-", ".--",  "-..-", "-.--", "--.."
};

char morse_letter(char c)
{
    if ((c >= 'a') && (c <= 'z')) {
        return (char)(c - 'a' + 'A');
    }
    return ((c >= 'A') && (c <= 'Z')) ? c : '\0';
}

bool morse_encode(char c, morse_symbol_t out[MORSE_LETTER_MAX_LEN], uint8_t *len)
{
    const char letter = morse_letter(c);

    if (letter == '\0') {
        return false;
    }

    const char *code = b_codes[letter - 'A'];
    uint8_t n = 0u;

    for (; code[n] != '\0'; ++n) {
        out[n] = (code[n] == '-') ? MORSE_DASH : MORSE_DOT;
    }
    *len = n;
    return true;
}

char morse_decode(const morse_symbol_t *seq, uint8_t len)
{
    char text[MORSE_LETTER_MAX_LEN + 1u];

    if ((len == 0u) || (len > MORSE_LETTER_MAX_LEN)) {
        return '\0';
    }
    morse_to_text(seq, len, text);

    for (uint8_t i = 0u; i < 26u; ++i) {
        if (strcmp(text, b_codes[i]) == 0) {
            return (char)('A' + i);
        }
    }
    return '\0';
}

void morse_to_text(const morse_symbol_t *seq, uint8_t len, char *out)
{
    for (uint8_t i = 0u; i < len; ++i) {
        out[i] = (seq[i] == MORSE_DASH) ? '-' : '.';
    }
    out[len] = '\0';
}
