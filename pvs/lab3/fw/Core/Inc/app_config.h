#ifndef APP_CONFIG_H
#define APP_CONFIG_H

/*
 * Единственное место, где заданы параметры приложения.
 */

/* --- Последовательный канал --- */
#define SERIAL_BAUD          115200u
#define SERIAL_RX_BUF_SIZE       64u   /* ёмкость очереди приёма, байт */
#define SERIAL_TX_BUF_SIZE     4096u   /* ёмкость очереди передачи, байт */
#define SERIAL_IRQ_PRIORITY       5u   /* выше системного таймера (15) */
#define SERIAL_START_MODE   SERIAL_MODE_IRQ

/* Самое длинное сообщение программы вместе с переводами строк */
#define CONSOLE_LINE_MAX         96u

/* --- Драйвер кнопки: счётчик антидребезга с гистерезисом --- */
#define BTN_POLL_MS              1u    /* шаг счётчика */
#define BTN_CNT_MAX             25u
#define BTN_CNT_PRESS           20u    /* нажатие: 20 мс устойчивого замыкания */
#define BTN_CNT_RELEASE          5u    /* отпускание: 20 мс устойчивого размыкания */

/* --- Шина I2C --- */
#define I2C_BUS_SPEED_HZ     400000u
/*
 * Прерывания I2C - самые приоритетные в программе: при чтении обработчик
 * должен успеть снять ACK и выставить STOP до конца приёма очередного байта
 * (RM0090, I2C master receiver, "closing the communication").
 */
#define I2C_IRQ_PRIORITY          1u
/* Обмены по опросу при инициализации; тайм-аут HAL - на всю транзакцию, а полный кадр идёт ~23 мс */
#define I2C_BLOCKING_TIMEOUT_MS 100u
#define I2C_TXN_TIMEOUT_MS        5u   /* дольше транзакция не идёт: шина перезапускается */

/*
 * --- Расписание обмена по I2C ---
 * Таймер расписания запускает не больше одной транзакции за такт. Состав
 * тактов задан таблицей в i2c_sched.c.
 */
#define SCHED_TICK_US          1000u
#define SCHED_IRQ_PRIORITY        6u   /* ниже UART (5) и I2C (1) */

/* Кадр видеопамяти передаётся частями: каждая должна укладываться в такт расписания */
#define OLED_CHUNK_BYTES         32u

/*
 * --- Клавиатура: антидребезг счётчиком с гистерезисом ---
 * Шаг счётчика - один полный опрос матрицы (период цикла расписания).
 */
#define KBD_CNT_MAX               3u
#define KBD_CNT_PRESS             2u   /* нажатие: два опроса подряд кнопка замкнута */
#define KBD_CNT_RELEASE           0u   /* отпускание: счётчик опустился до нуля */
#define KBD_EVENT_QUEUE           8u

/* --- Прикладная часть --- */
#define ACC_REPORT_PERIOD_MS     50u   /* период отправки показаний на ПК */
#define ACC_STALE_MS            200u   /* дольше нет новых данных - датчик считается неисправным */
#define PROTO_LINE_MAX           32u   /* самая длинная команда от ПК */

/*
 * Проекция силы тяжести на плоскость экрана меньше этого значения (стенд
 * лежит почти горизонтально) - направление указателя не определено.
 */
#define LEVEL_FLAT_MG           100

#endif /* APP_CONFIG_H */
