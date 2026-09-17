#include <stddef.h>

#include "app_main.h"
#include "app_config.h"
#include "board.h"
#include "button.h"
#include "led.h"
#include "morse_app.h"
#include "sched.h"

/*
 * Верхний уровень программы.
 *
 * Здесь нет ни номеров выводов, ни обращений к регистрам, ни пауз с активным
 * ожиданием. Три независимых процесса выполняются псевдопараллельно: каждый
 * делает короткий шаг и возвращает управление планировщику.
 */
static sched_task_t b_tasks[] = {
    { button_task,    NULL, BTN_POLL_MS, 0u },  /* опрос кнопки и антидребезг */
    { led_task,       NULL, 1u,          0u },  /* выдержки времени индикации */
    { morse_app_task, NULL, 0u,          0u }   /* логика варианта задания    */
};

void app_setup(void)
{
    board_init();
    led_init();
    button_init();
    morse_app_init();

    sched_init(b_tasks, (uint8_t)(sizeof(b_tasks) / sizeof(b_tasks[0])));
}

void app_step(void)
{
    sched_run_once();
}

void app_main(void)
{
    app_setup();
    sched_run_forever();
}
