#ifndef APP_CONFIG_H
#define APP_CONFIG_H

/*
 * Единственное место, где заданы параметры приложения.
 */

/* --- Последовательный канал --- */
#define SERIAL_BAUD          115200u   /* по заданию варианта 3 */
#define SERIAL_RX_BUF_SIZE       64u   /* ёмкость очереди приёма, байт */
/*
 * Ёмкость очереди передачи, байт. На каждый принятый символ уходит до ~30 байт
 * (эхо и сообщение), а передаются они в 11,5 раз медленнее, чем приходят
 * вставленные из буфера обмена строки; 4 КБ хватает на вставку ~140 символов.
 */
#define SERIAL_TX_BUF_SIZE     4096u
#define SERIAL_IRQ_PRIORITY       5u   /* выше системного таймера (15) */
#define SERIAL_START_MODE   SERIAL_MODE_IRQ

/* Самое длинное сообщение программы вместе с переводами строк */
#define CONSOLE_LINE_MAX         96u

/* --- Драйвер кнопки: счётчик антидребезга с гистерезисом --- */
#define BTN_POLL_MS              1u    /* шаг счётчика */
#define BTN_CNT_MAX             25u
#define BTN_CNT_PRESS           20u    /* нажатие: 20 мс устойчивого замыкания */
#define BTN_CNT_RELEASE          5u    /* отпускание: 20 мс устойчивого размыкания */

/* --- Ввод кнопкой --- */
#define MORSE_DASH_THRESHOLD_MS 300u   /* короче - точка, не короче - тире */
#define MORSE_LETTER_GAP_MS    1000u   /* пауза после нажатия, завершающая букву */
#define MORSE_FEEDBACK_MS       120u   /* мерцание двухцветного светодиода после нажатия */
#define MORSE_ERR_STEP_MS        60u   /* индикация лишнего нажатия */
#define MORSE_ERR_CYCLES          6u

/* Сколько нажатий запоминается; длиннее буквы латиницы, чтобы сообщить о неизвестной */
#define MORSE_CAPACITY            8u

/* --- Проигрывание символов, принятых по UART --- */
#define MORSE_TX_QUEUE_LEN        8u   /* по заданию: буферизация до восьми символов */

/*
 * T - базовая единица (длительность точки). Канон азбуки Морзе: тире = 3T,
 * интервал между элементами знака = 1T, между знаками = 3T.
 */
#define MORSE_UNIT_MS           200u
#define MORSE_DOT_UNITS           1u
#define MORSE_DASH_UNITS          3u
#define MORSE_GAP_UNITS           1u
#define MORSE_LEAD_IN_UNITS       0u
#define MORSE_TAIL_UNITS          3u   /* интервал перед следующим знаком */

#endif /* APP_CONFIG_H */
