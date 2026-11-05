#!/bin/sh
# Сборка прошивки для стенда SDK-1.1M (STM32F427VIT6), лабораторная работа 3.
set -e

ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"

TC="/c/Program Files (x86)/Arm GNU Toolchain arm-none-eabi/14.2 rel1/bin"
# Пакет STM32CubeF4 уже скачан для лабораторной работы 1, повторно не копируется
CUBE=${CUBE:-../lab1/third_party/STM32CubeF4/Drivers}
HAL=$CUBE/STM32F4xx_HAL_Driver/Src

OUT=fw/build
mkdir -p "$OUT"

CFLAGS="-mcpu=cortex-m4 -mthumb -mfpu=fpv4-sp-d16 -mfloat-abi=hard \
 -DUSE_HAL_DRIVER -DSTM32F427xx -DHSE_VALUE=25000000 \
 -O2 -g3 -Wall -Wextra -ffunction-sections -fdata-sections -std=gnu11"

INCLUDES="-Ifw/Core/Inc \
 -I$CUBE/STM32F4xx_HAL_Driver/Inc \
 -I$CUBE/CMSIS/Device/ST/STM32F4xx/Include \
 -I$CUBE/CMSIS/Include"

SOURCES="fw/Core/Src/main.c \
 fw/Core/Src/stm32f4xx_it.c \
 fw/Core/Src/app_main.c \
 fw/Core/Src/sys_tick_hal.c \
 fw/Core/Src/board.c \
 fw/Core/Src/gpio_hw_hal.c \
 fw/Core/Src/led.c \
 fw/Core/Src/button.c \
 fw/Core/Src/swtimer.c \
 fw/Core/Src/fifo.c \
 fw/Core/Src/serial.c \
 fw/Core/Src/console.c \
 fw/Core/Src/uart_hal.c \
 fw/Core/Src/uart_hal_it.c \
 fw/Core/Src/uart_hal_poll.c \
 fw/Core/Src/i2c_bus.c \
 fw/Core/Src/i2c_sched.c \
 fw/Core/Src/accel.c \
 fw/Core/Src/keypad.c \
 fw/Core/Src/oled.c \
 fw/Core/Src/gfx.c \
 fw/Core/Src/font5x7.c \
 fw/Core/Src/level.c \
 fw/Core/Src/proto.c \
 $CUBE/CMSIS/Device/ST/STM32F4xx/Source/Templates/system_stm32f4xx.c \
 $CUBE/CMSIS/Device/ST/STM32F4xx/Source/Templates/gcc/startup_stm32f427xx.s \
 $HAL/stm32f4xx_hal.c \
 $HAL/stm32f4xx_hal_cortex.c \
 $HAL/stm32f4xx_hal_rcc.c \
 $HAL/stm32f4xx_hal_rcc_ex.c \
 $HAL/stm32f4xx_hal_gpio.c \
 $HAL/stm32f4xx_hal_pwr.c \
 $HAL/stm32f4xx_hal_pwr_ex.c \
 $HAL/stm32f4xx_hal_dma.c \
 $HAL/stm32f4xx_hal_uart.c \
 $HAL/stm32f4xx_hal_i2c.c \
 $HAL/stm32f4xx_hal_tim.c \
 $HAL/stm32f4xx_hal_tim_ex.c"

"$TC/arm-none-eabi-gcc.exe" $CFLAGS $INCLUDES $SOURCES \
    -T fw/STM32F427VITX_FLASH.ld \
    --specs=nano.specs --specs=nosys.specs \
    -Wl,--gc-sections -Wl,--no-warn-rwx-segments -Wl,-Map="$OUT/lab3.map" -Wl,--print-memory-usage \
    -o "$OUT/lab3.elf" -lm

"$TC/arm-none-eabi-objcopy.exe" -O ihex   "$OUT/lab3.elf" "$OUT/lab3.hex"
"$TC/arm-none-eabi-objcopy.exe" -O binary "$OUT/lab3.elf" "$OUT/lab3.bin"
"$TC/arm-none-eabi-size.exe" "$OUT/lab3.elf"
echo "собрано: $OUT/lab3.elf"
