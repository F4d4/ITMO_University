#ifndef PCA9538_H
#define PCA9538_H

/* Регистры расширителя портов PCA9538 (командный байт после адреса) */
#define PCA9538_REG_INPUT     0x00u   /* уровни на выводах, только чтение */
#define PCA9538_REG_OUTPUT    0x01u   /* уровни для выводов-выходов */
#define PCA9538_REG_POLARITY  0x02u   /* инверсия входов */
#define PCA9538_REG_CONFIG    0x03u   /* 1 - вход, 0 - выход; после сброса все входы */

#endif /* PCA9538_H */
