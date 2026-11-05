#ifndef CONSOLE_H
#define CONSOLE_H

#include <stdbool.h>
#include <stdint.h>

/*
 * Текстовый вывод в терминал поверх последовательного канала.
 *
 * Следит, где находится курсор терминала, чтобы каждое сообщение стенда
 * начиналось с новой строки, но лишние пустые строки не появлялись.
 */

/* Эхо принятого символа; Enter (CR, LF или CR LF) переводит строку */
void console_echo(uint8_t c);

/*
 * Сообщение отдельной строкой (формат как у printf). false - в очереди
 * передачи не хватило места, сообщение отброшено целиком.
 */
bool console_message(const char *fmt, ...) __attribute__((format(printf, 1, 2)));

#endif /* CONSOLE_H */
