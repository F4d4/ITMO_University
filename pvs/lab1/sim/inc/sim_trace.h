#ifndef SIM_TRACE_H
#define SIM_TRACE_H

#include <stdint.h>
#include <stdbool.h>

#include "gpio_sim.h"

bool sim_trace_open (const char *log_path, const char *js_path, const char *scenario);
void sim_trace_close(void);

void sim_trace_reg(char rw, uint8_t port, uint32_t offset, uint32_t value, uint32_t old_value);
void sim_trace_pad(uint8_t port, uint8_t pin, pad_level_t level, const char *note);
void sim_trace_app(const char *fmt, ...);
void sim_trace_violation(const char *fmt, ...);

uint32_t sim_trace_violation_count(void);

#endif /* SIM_TRACE_H */
