#include <stdbool.h>
#include <stddef.h>

#include "morse_app.h"
#include "app_config.h"
#include "button.h"
#include "console.h"
#include "fifo.h"
#include "led.h"
#include "morse_buffer.h"
#include "morse_codec.h"
#include "morse_player.h"
#include "serial.h"
#include "swtimer.h"

/*
 * Вариант 3, "Азбука Морзе", лабораторная работа 2.
 *
 * Три независимых процесса, каждый делает короткий шаг и сразу возвращает
 * управление, поэтому все они идут одновременно:
 *
 *  1. Приём с кнопки. Короткое нажатие - точка, длинное - тире; после
 *     каждого нажатия двухцветный светодиод коротко показывает введённый
 *     элемент (точка - жёлтым, тире - красным). Пауза после последнего
 *     нажатия завершает знак: он дешифруется, и в канал выводится
 *     "received symbol: X".
 *
 *  2. Обработка символов из последовательного канала. Каждый символ
 *     отсылается обратно (эхо). Буква латинского алфавита ставится в очередь
 *     на проигрывание (до восьми символов), в канал выводится
 *     "transmitting symbol: X". Символ '+' включает или выключает
 *     прерывания UART с сообщением "interrupts on" / "interrupts off".
 *
 *  3. Проигрывание. Символы из очереди по одному кодируются и передаются
 *     зелёным светодиодом короткими (точка) и длинными (тире) импульсами.
 *
 * В этом файле нет обращений к аппаратуре: только драйверы кнопки,
 * светодиодов и последовательного канала.
 */

/* --- Приём с кнопки --- */
static morse_buffer_t b_input;
static swtimer_t      b_letter_gap;
static uint32_t       b_press_ms;
static bool           b_press_valid;

/* --- Проигрывание --- */
static uint8_t        b_queue_mem[FIFO_STORAGE_SIZE(MORSE_TX_QUEUE_LEN)];
static fifo_t         b_queue;
static morse_player_t b_player;

static const morse_timing_t b_timing = {
    .unit_ms       = MORSE_UNIT_MS,
    .dot_units     = MORSE_DOT_UNITS,
    .dash_units    = MORSE_DASH_UNITS,
    .gap_units     = MORSE_GAP_UNITS,
    .lead_in_units = MORSE_LEAD_IN_UNITS,
    .tail_units    = MORSE_TAIL_UNITS
};

/* Абстрактный выход передатчика подключён к зелёному светодиоду */
static void morse_app_sink(bool on, void *ctx)
{
    (void)ctx;
    led_set(LED_CH_GREEN, on ? LED_ON : LED_OFF);
}

/* ---------------------------------------------------------------------------
 * 1. Приём с кнопки
 * ------------------------------------------------------------------------- */

static void morse_app_on_press(uint32_t t_ms)
{
    b_press_ms    = t_ms;
    b_press_valid = true;

    /*
     * Отсчёт паузы прерывается на время удержания кнопки: иначе нажатие
     * длиннее паузы завершило бы знак прямо посреди ввода тире.
     */
    swtimer_stop(&b_letter_gap);
}

static void morse_app_on_release(uint32_t t_ms)
{
    if (!b_press_valid) {
        return;
    }
    b_press_valid = false;

    const uint32_t       duration = (uint32_t)(t_ms - b_press_ms);
    const morse_symbol_t symbol   = (duration < MORSE_DASH_THRESHOLD_MS) ? MORSE_DOT
                                                                         : MORSE_DASH;

    if (morse_buffer_push(&b_input, symbol)) {
        led_pulse(LED_CH_BICOLOR, (symbol == MORSE_DOT) ? LED_YELLOW : LED_RED,
                  MORSE_FEEDBACK_MS);
    } else {
        /* Лишние нажатия не запоминаются, о чём сообщает частое мигание красным */
        led_blink(LED_CH_BICOLOR, LED_RED,
                  MORSE_ERR_STEP_MS, MORSE_ERR_STEP_MS, MORSE_ERR_CYCLES);
    }

    swtimer_start(&b_letter_gap, MORSE_LETTER_GAP_MS);
}

static void morse_app_finish_letter(void)
{
    const morse_symbol_t *seq    = morse_buffer_data(&b_input);
    const uint8_t         len    = morse_buffer_len(&b_input);
    const char            letter = morse_decode(seq, len);

    if (letter != '\0') {
        (void)console_message("received symbol: %c", letter);
    } else {
        char text[MORSE_CAPACITY + 1u];

        morse_to_text(seq, len, text);
        (void)console_message("unknown sequence: %s", text);
    }
    morse_buffer_clear(&b_input);
}

static void morse_app_input_task(void)
{
    btn_event_t event;

    while (button_pop(&event)) {
        if (event.type == BTN_EV_PRESSED) {
            morse_app_on_press(event.t_ms);
        } else {
            morse_app_on_release(event.t_ms);
        }
    }

    if (swtimer_expired(&b_letter_gap)) {
        morse_app_finish_letter();
    }
}

/* ---------------------------------------------------------------------------
 * 2. Символы из последовательного канала
 * ------------------------------------------------------------------------- */

static void morse_app_toggle_interrupts(void)
{
    const bool irq_on = (serial_get_mode() != SERIAL_MODE_IRQ);

    serial_set_mode(irq_on ? SERIAL_MODE_IRQ : SERIAL_MODE_POLLING);
    (void)console_message("%s", irq_on ? "interrupts on" : "interrupts off");
}

static void morse_app_on_char(uint8_t c)
{
    /* Проигрывается и сообщается заглавная буква: регистра в азбуке Морзе нет */
    const char letter = morse_letter((char)c);

    if (c == '+') {
        morse_app_toggle_interrupts();
    } else if (letter != '\0') {
        if (fifo_put(&b_queue, (uint8_t)letter)) {
            (void)console_message("transmitting symbol: %c", letter);
        } else {
            (void)console_message("queue is full, symbol %c ignored", letter);
        }
    } else if ((c == '\r') || (c == '\n') || (c == ' ')) {
        /* Перевод строки и пробел разделяют ввод, передавать в них нечего */
    } else if ((c >= 0x20u) && (c < 0x7Fu)) {
        (void)console_message("unsupported symbol: %c", (char)c);
    } else {
        (void)console_message("unsupported symbol: 0x%02X", (unsigned)c);
    }
}

static void morse_app_serial_task(void)
{
    uint8_t c;

    while (serial_read(&c)) {
        console_echo(c);
        morse_app_on_char(c);
    }
}

/* ---------------------------------------------------------------------------
 * 3. Проигрывание зелёным светодиодом
 * ------------------------------------------------------------------------- */

static void morse_app_output_task(void)
{
    morse_player_task(&b_player);

    if (morse_player_busy(&b_player)) {
        return;
    }

    uint8_t letter;

    if (fifo_get(&b_queue, &letter)) {
        morse_symbol_t seq[MORSE_LETTER_MAX_LEN];
        uint8_t        len;

        /* В очередь попадают только буквы, поэтому кодирование всегда успешно */
        (void)morse_encode((char)letter, seq, &len);
        (void)morse_player_start(&b_player, seq, len);
    }
}

/* ------------------------------------------------------------------------- */

void morse_app_init(void)
{
    b_press_valid = false;
    b_press_ms    = 0u;
    morse_buffer_clear(&b_input);
    swtimer_stop(&b_letter_gap);

    fifo_init(&b_queue, b_queue_mem, (uint16_t)sizeof(b_queue_mem));
    morse_player_init(&b_player, &b_timing, morse_app_sink, NULL);

    (void)console_message("SDK-1.1M, lab 2, variant 3: Morse code over UART");
    (void)console_message("Type Latin letters A-Z to transmit them with the green LED.");
    (void)console_message("Button: short press - dot, long press - dash, pause - end of symbol.");
    (void)console_message("'+' toggles UART interrupts.");
    (void)console_message("%s", (serial_get_mode() == SERIAL_MODE_IRQ) ? "interrupts on"
                                                                       : "interrupts off");
}

void morse_app_task(void)
{
    morse_app_input_task();
    morse_app_serial_task();
    morse_app_output_task();
}
