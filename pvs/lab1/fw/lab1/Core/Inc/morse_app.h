#ifndef MORSE_APP_H
#define MORSE_APP_H

/*
 * Прикладная логика варианта 3 - "передатчик" азбуки Морзе.
 * Единственный модуль, в котором собрана логика задания.
 */

void morse_app_init(void);

/* Шаг прикладного автомата; вызывается планировщиком */
void morse_app_task(void *ctx);

#endif /* MORSE_APP_H */
