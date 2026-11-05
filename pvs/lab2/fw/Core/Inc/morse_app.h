#ifndef MORSE_APP_H
#define MORSE_APP_H

/*
 * Прикладная логика варианта 3 - "Азбука Морзе" с обменом по UART.
 * Единственный модуль, в котором собрана логика задания.
 */

void morse_app_init(void);

/* Шаг прикладного автомата; вызывается планировщиком */
void morse_app_task(void);

#endif /* MORSE_APP_H */
