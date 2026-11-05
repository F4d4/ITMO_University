#ifndef FONT5X7_H
#define FONT5X7_H

#include <stdint.h>

/* Печатные символы ASCII 0x20..0x7F */
#define FONT5X7_FIRST  0x20
#define FONT5X7_COUNT  96
#define FONT5X7_ROWS   7

extern const uint8_t font5x7[FONT5X7_COUNT][FONT5X7_ROWS];

#endif /* FONT5X7_H */
