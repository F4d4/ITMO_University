#ifndef SERIAL_H
#define SERIAL_H

#include <stdint.h>
#include <stdbool.h>

/*
 * Последовательный канал связи с компьютером.
 *
 * Владеет буферами приёма и передачи (их размеры задаются в app_config.h) и
 * выбирает драйвер UART: по прерываниям или по опросу. Режим можно менять на
 * ходу; данные, накопленные в буферах, при этом сохраняются.
 *
 * Все функции неблокирующие.
 */

typedef enum {
    SERIAL_MODE_POLLING = 0,
    SERIAL_MODE_IRQ
} serial_mode_t;

void          serial_init(serial_mode_t mode);

void          serial_set_mode(serial_mode_t mode);
serial_mode_t serial_get_mode(void);

/* Шаг обслуживания; вызывается из главного цикла как можно чаще */
void          serial_task(void);

/* Очередной принятый байт; false - принятых данных нет */
bool          serial_read(uint8_t *byte);

/*
 * Поставить данные в очередь на отправку. Всё или ничего: если места не
 * хватает, данные отбрасываются целиком и возвращается false - так в канал
 * не попадают оборванные на полуслове сообщения.
 */
bool          serial_write(const void *data, uint16_t len);

#endif /* SERIAL_H */
