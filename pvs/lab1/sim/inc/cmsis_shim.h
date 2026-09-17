#ifndef CMSIS_SHIM_H
#define CMSIS_SHIM_H

#include <stdint.h>

/*
 * Заглушка описаний CMSIS для сборки на инструментальной машине.
 *
 * Раскладка GPIO_TypeDef и базовые адреса в точности соответствуют
 * STM32F427 (RM0090, раздел "General purpose I/O (GPIO)"), поэтому драйвер
 * mygpio компилируется без единого изменения. Сами указатели никогда не
 * разыменовываются: доступ идёт через gpio_bus_read32 / gpio_bus_write32,
 * которые перехватывает модель аппаратуры.
 */
typedef struct {
    volatile uint32_t MODER;    /* 0x00 режим работы вывода            */
    volatile uint32_t OTYPER;   /* 0x04 тип выходного каскада          */
    volatile uint32_t OSPEEDR;  /* 0x08 крутизна фронтов               */
    volatile uint32_t PUPDR;    /* 0x0C подтягивающие резисторы        */
    volatile uint32_t IDR;      /* 0x10 входные данные, только чтение  */
    volatile uint32_t ODR;      /* 0x14 выходная защёлка               */
    volatile uint32_t BSRR;     /* 0x18 установка/сброс, только запись */
    volatile uint32_t LCKR;     /* 0x1C блокировка конфигурации        */
    volatile uint32_t AFR[2];   /* 0x20 альтернативные функции         */
} GPIO_TypeDef;

#define PERIPH_BASE      0x40000000UL
#define AHB1PERIPH_BASE  (PERIPH_BASE + 0x00020000UL)

#define GPIOA_BASE  (AHB1PERIPH_BASE + 0x0000UL)
#define GPIOB_BASE  (AHB1PERIPH_BASE + 0x0400UL)
#define GPIOC_BASE  (AHB1PERIPH_BASE + 0x0800UL)
#define GPIOD_BASE  (AHB1PERIPH_BASE + 0x0C00UL)
#define GPIOE_BASE  (AHB1PERIPH_BASE + 0x1000UL)

#define GPIOA  ((GPIO_TypeDef *)GPIOA_BASE)
#define GPIOB  ((GPIO_TypeDef *)GPIOB_BASE)
#define GPIOC  ((GPIO_TypeDef *)GPIOC_BASE)
#define GPIOD  ((GPIO_TypeDef *)GPIOD_BASE)
#define GPIOE  ((GPIO_TypeDef *)GPIOE_BASE)

#endif /* CMSIS_SHIM_H */
