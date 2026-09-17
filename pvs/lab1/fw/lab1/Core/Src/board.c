#include "board.h"
#include "board_pins.h"
#include "gpio_hw.h"

/*
 * Таблица "логический цвет канала -> (маска выводов, уровни на них)".
 *
 * Это единственное место, где закодирована электрическая схема индикации.
 * После пробы полярности (fw/probe) правится только эта таблица и константа
 * BOARD_LED_GREEN_ACTIVE_LEVEL в board_pins.h.
 *
 * Для двухцветного светодиода, включённого встречно-параллельно, цвет задаётся
 * направлением тока: один вывод в единице, другой в нуле. Комбинация "оба в
 * нуле" гасит прибор. Оба вывода всегда остаются выходами - на это прямо
 * указывает предупреждение учебного пособия о том, что второй контакт
 * необходимо сохранять выходом с нулём, а не оставлять неуправляемым.
 */
typedef struct {
    uint16_t mask;
    uint16_t values;
} board_led_pattern_t;

#define GREEN_ON_VALUES   ((BOARD_LED_GREEN_ACTIVE_LEVEL != 0u) ? BOARD_LED_GREEN_MASK : 0u)
#define GREEN_OFF_VALUES  ((BOARD_LED_GREEN_ACTIVE_LEVEL != 0u) ? 0u : BOARD_LED_GREEN_MASK)

static const board_led_pattern_t b_led_patterns[LED_CH_COUNT][LED_COLOR_COUNT] = {
    [LED_CH_GREEN] = {
        [LED_OFF] = { BOARD_LED_GREEN_MASK, GREEN_OFF_VALUES },
        [LED_ON]  = { BOARD_LED_GREEN_MASK, GREEN_ON_VALUES  },
        /* у одноцветного канала третьего состояния нет, трактуем как выключение */
        [LED_RED] = { BOARD_LED_GREEN_MASK, GREEN_OFF_VALUES }
    },
    [LED_CH_BICOLOR] = {
        [LED_OFF]    = { BOARD_LED_BI_MASK, 0u                  },
        [LED_YELLOW] = { BOARD_LED_BI_MASK, BOARD_LED_BI_A_MASK },
        [LED_RED]    = { BOARD_LED_BI_MASK, BOARD_LED_BI_B_MASK }
    }
};

void board_init(void)
{
    gpio_hw_port_enable(BOARD_LED_PORT);
    gpio_hw_port_enable(BOARD_BTN_PORT);

    /*
     * Светодиоды инициализируются сразу погашенными: уровень задаётся до
     * включения выходного буфера, поэтому при старте программы на выводах
     * не возникает кратковременной вспышки.
     */
    gpio_hw_config_mask(BOARD_LED_PORT, BOARD_LED_ALL_MASK,
                        GPIO_HW_OUT_PP, GPIO_HW_NOPULL,
                        (GREEN_OFF_VALUES != 0u) ? 1u : 0u);

    board_led_apply(LED_CH_GREEN,   LED_OFF);
    board_led_apply(LED_CH_BICOLOR, LED_OFF);

    gpio_hw_config_mask(BOARD_BTN_PORT, BOARD_BTN_MASK,
                        GPIO_HW_IN,
                        (BOARD_BTN_ACTIVE_LEVEL == 0u) ? GPIO_HW_PULLUP : GPIO_HW_PULLDOWN,
                        GPIO_HW_LEVEL_KEEP);
}

void board_led_apply(led_ch_t ch, led_color_t color)
{
    if ((ch >= LED_CH_COUNT) || (color >= LED_COLOR_COUNT)) {
        return;
    }

    const board_led_pattern_t *pattern = &b_led_patterns[ch][color];

    /* Одна запись: оба вывода двухцветного светодиода меняются одновременно */
    gpio_hw_write_mask(BOARD_LED_PORT, pattern->mask, pattern->values);
}

bool board_button_is_closed(void)
{
    const uint16_t level = gpio_hw_read_mask(BOARD_BTN_PORT, BOARD_BTN_MASK);

    return (BOARD_BTN_ACTIVE_LEVEL == 0u) ? (level == 0u) : (level != 0u);
}
