#ifndef SIM_TIME_H
#define SIM_TIME_H

#include <stdint.h>

void     sim_time_reset(void);
uint32_t sim_time_now(void);
void     sim_time_advance(uint32_t ms);

#endif /* SIM_TIME_H */
