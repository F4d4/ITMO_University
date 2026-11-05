#include <stdarg.h>
#include <stdio.h>

#include "console.h"
#include "app_config.h"
#include "serial.h"

static bool    b_line_open;   /* в текущей строке терминала уже есть эхо */
static uint8_t b_prev;        /* предыдущий принятый символ */

void console_echo(uint8_t c)
{
    if ((c == '\r') || (c == '\n')) {
        /* Терминал может отправлять Enter как CR LF - это один перевод строки */
        const bool second_half = (c == '\n') && (b_prev == '\r');

        if (!second_half && serial_write("\r\n", 2u)) {
            b_line_open = false;
        }
    } else if (serial_write(&c, 1u)) {
        b_line_open = true;
    }
    b_prev = c;
}

bool console_message(const char *fmt, ...)
{
    char    line[CONSOLE_LINE_MAX];
    int     len = 0;
    va_list args;

    if (b_line_open) {
        line[len++] = '\r';
        line[len++] = '\n';
    }

    /* Два последних байта буфера оставлены под завершающий перевод строки */
    const size_t room = sizeof(line) - (size_t)len - 2u;

    va_start(args, fmt);
    int body = vsnprintf(&line[len], room, fmt, args);
    va_end(args);

    /* vsnprintf сообщает полную длину; не поместившийся хвост обрезан */
    if ((size_t)body >= room) {
        body = (int)room - 1;
    }
    len += body;
    line[len++] = '\r';
    line[len++] = '\n';

    if (!serial_write(line, (uint16_t)len)) {
        return false;
    }
    b_line_open = false;
    return true;
}
