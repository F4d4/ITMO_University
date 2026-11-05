#include <ctype.h>
#include <stdbool.h>
#include <string.h>

#include "proto.h"
#include "app_config.h"
#include "console.h"
#include "serial.h"

static char     b_line[PROTO_LINE_MAX + 1u];
static uint32_t b_len;
static bool     b_overflow;   /* строка длиннее буфера: дочитываем до конца и отвергаем */

static proto_cmd_t proto_parse(const char *s)
{
    if (strcmp(s, "PING") == 0) {
        return PROTO_CMD_PING;
    }
    if (strcmp(s, "MODE H") == 0) {
        return PROTO_CMD_MODE_HORIZON;
    }
    if (strcmp(s, "MODE A") == 0) {
        return PROTO_CMD_MODE_ARROW;
    }
    return PROTO_CMD_INVALID;
}

proto_cmd_t proto_poll(void)
{
    uint8_t c;

    while (serial_read(&c)) {
        if ((c == '\r') || (c == '\n')) {
            const bool overflow = b_overflow;
            const bool empty    = (b_len == 0u);

            b_line[b_len] = '\0';
            b_len         = 0u;
            b_overflow    = false;

            if (overflow) {
                return PROTO_CMD_INVALID;
            }
            if (!empty) {   /* пустая строка - вторая половина CR LF */
                return proto_parse(b_line);
            }
        } else if (b_len < PROTO_LINE_MAX) {
            b_line[b_len++] = (char)toupper(c);
        } else {
            b_overflow = true;
        }
    }
    return PROTO_CMD_NONE;
}

void proto_send_pong(void)
{
    (void)console_message("PONG");
}

void proto_send_mode_ok(level_mode_t mode)
{
    (void)console_message("OK MODE %c", level_mode_letter(mode));
}

void proto_send_error(void)
{
    (void)console_message("ERR SYNTAX");
}

void proto_send_acc(const level_view_t *v, level_mode_t mode)
{
    const char m = level_mode_letter(mode);

    if (!v->valid) {
        (void)console_message("ACC - - - %c -", m);
    } else if (v->flat) {
        (void)console_message("ACC %d %d %d %c -", v->mg[0], v->mg[1], v->mg[2], m);
    } else {
        (void)console_message("ACC %d %d %d %c %d", v->mg[0], v->mg[1], v->mg[2], m, v->angle_deg);
    }
}

void proto_send_key(uint8_t code)
{
    (void)console_message("KEY %u", (unsigned)code);
}
