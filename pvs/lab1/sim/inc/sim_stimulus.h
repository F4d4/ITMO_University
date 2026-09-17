#ifndef SIM_STIMULUS_H
#define SIM_STIMULUS_H

#include <stddef.h>
#include <stdint.h>

#include "gpio_sim.h"

/*
 * Программируемое изменение состояния входных выводов в заданные моменты
 * модельного времени.
 */
typedef struct {
    uint32_t    t_ms;
    uint8_t     port;
    uint8_t     pin;
    pad_drive_t drive;
    const char *note;
} stim_event_t;

void   stim_load(const stim_event_t *events, size_t count);
void   stim_apply_until(uint32_t t_ms);
size_t stim_pending(void);

#endif /* SIM_STIMULUS_H */
