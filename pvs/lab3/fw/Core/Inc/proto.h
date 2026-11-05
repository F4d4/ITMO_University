#ifndef PROTO_H
#define PROTO_H

#include <stdint.h>

#include "level.h"

/*
 * Протокол обмена с клиентской программой на ПК.
 *
 * Текстовые строки ASCII. Стенд принимает строку, оканчивающуюся CR, LF
 * или CR LF, регистр букв не важен; сам отправляет строки с CR LF.
 *
 * ПК -> стенд:
 *   PING         проверка связи                   ответ: PONG
 *   MODE H       указатель - линия горизонта      ответ: OK MODE H
 *   MODE A       указатель - стрелка вниз         ответ: OK MODE A
 *   другое                                        ответ: ERR SYNTAX
 *
 * Стенд -> ПК:
 *   ACC <x> <y> <z> <m> <deg>   каждые ACC_REPORT_PERIOD_MS:
 *       x, y, z - ускорение по осям датчика в mg (целые со знаком);
 *       m       - текущий тип указателя на стенде: H или A;
 *       deg     - направление силы тяжести на экране в градусах 0..359
 *                 (0 - вправо, 90 - вниз, по часовой стрелке) либо "-",
 *                 если стенд лежит горизонтально и направление не определено.
 *       Нет показаний датчика: x, y, z и deg равны "-".
 *   KEY <n>      нажата кнопка клавиатуры стенда с кодом n (1..12)
 *   ERR INIT <dev>   при запуске: устройство не ответило (OLED, ACCEL, KEYPAD)
 */

typedef enum {
    PROTO_CMD_NONE = 0,     /* полной строки ещё нет */
    PROTO_CMD_PING,
    PROTO_CMD_MODE_HORIZON,
    PROTO_CMD_MODE_ARROW,
    PROTO_CMD_INVALID
} proto_cmd_t;

/* Разобрать принятые байты; возвращает команду, как только пришла полная строка */
proto_cmd_t proto_poll(void);

void proto_send_pong(void);
void proto_send_mode_ok(level_mode_t mode);
void proto_send_error(void);
void proto_send_acc(const level_view_t *v, level_mode_t mode);
void proto_send_key(uint8_t code);

#endif /* PROTO_H */
