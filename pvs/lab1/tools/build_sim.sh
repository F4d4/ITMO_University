#!/bin/sh
# Сборка симулятора host-компилятором
set -e
ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"
export PATH="/c/Users/verdi/AppData/Local/Microsoft/WinGet/Packages/BrechtSanders.WinLibs.POSIX.UCRT_Microsoft.Winget.Source_8wekyb3d8bbwe/mingw64/bin:$PATH"

gcc -std=c11 -Wall -Wextra -O0 -g -DSIM_BUILD -DGPIO_BACKEND_MY \
    -Ifw/lab1/Core/Inc -Isim/inc \
    fw/lab1/Core/Src/mygpio.c \
    fw/lab1/Core/Src/gpio_hw_my.c \
    fw/lab1/Core/Src/board.c \
    fw/lab1/Core/Src/led.c \
    fw/lab1/Core/Src/button.c \
    fw/lab1/Core/Src/swtimer.c \
    fw/lab1/Core/Src/sched.c \
    fw/lab1/Core/Src/morse_buffer.c \
    fw/lab1/Core/Src/morse_player.c \
    fw/lab1/Core/Src/morse_app.c \
    fw/lab1/Core/Src/app_main.c \
    sim/src/gpio_sim.c \
    sim/src/gpio_bus_sim.c \
    sim/src/naive_gpio.c \
    sim/src/sim_time.c \
    sim/src/sim_stimulus.c \
    sim/src/sim_trace.c \
    sim/src/sim_main.c \
    -o sim/build/sim_morse.exe
echo "собрано: sim/build/sim_morse.exe"
