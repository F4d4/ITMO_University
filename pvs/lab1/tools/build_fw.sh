#!/bin/sh
# Сборка прошивки для стенда SDK-1.1M (STM32F427VIT6).
#
# Аргумент: конфигурация сборки
#   hal   - драйвер GPIO из стандартной библиотеки HAL (по умолчанию)
#   mygpio - собственный регистровый драйвер GPIO (часть 2, способ 1)
set -e

ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"

CONFIG=${1:-hal}
case "$CONFIG" in
    hal)    BACKEND_DEF=-DGPIO_BACKEND_HAL; BACKEND_SRC=fw/lab1/Core/Src/gpio_hw_hal.c ;;
    mygpio) BACKEND_DEF=-DGPIO_BACKEND_MY;  BACKEND_SRC=fw/lab1/Core/Src/gpio_hw_my.c  ;;
    *)      echo "неизвестная конфигурация: $CONFIG (ожидается hal или mygpio)"; exit 2 ;;
esac

TC="/c/Program Files (x86)/Arm GNU Toolchain arm-none-eabi/14.2 rel1/bin"
CUBE=third_party/STM32CubeF4/Drivers
OUT=fw/build/$CONFIG
mkdir -p "$OUT"

CFLAGS="-mcpu=cortex-m4 -mthumb -mfpu=fpv4-sp-d16 -mfloat-abi=hard \
 -DUSE_HAL_DRIVER -DSTM32F427xx -DHSE_VALUE=25000000 $BACKEND_DEF \
 -O2 -g3 -Wall -Wextra -ffunction-sections -fdata-sections -std=gnu11"

INCLUDES="-Ifw/lab1/Core/Inc \
 -I$CUBE/STM32F4xx_HAL_Driver/Inc \
 -I$CUBE/CMSIS/Device/ST/STM32F4xx/Include \
 -I$CUBE/CMSIS/Include"

SOURCES="fw/lab1/Core/Src/main.c \
 fw/lab1/Core/Src/stm32f4xx_it.c \
 fw/lab1/Core/Src/app_main.c \
 fw/lab1/Core/Src/sys_tick_hal.c \
 fw/lab1/Core/Src/board.c \
 fw/lab1/Core/Src/led.c \
 fw/lab1/Core/Src/button.c \
 fw/lab1/Core/Src/swtimer.c \
 fw/lab1/Core/Src/sched.c \
 fw/lab1/Core/Src/morse_buffer.c \
 fw/lab1/Core/Src/morse_player.c \
 fw/lab1/Core/Src/morse_app.c \
 fw/lab1/Core/Src/mygpio.c \
 $BACKEND_SRC \
 $CUBE/CMSIS/Device/ST/STM32F4xx/Source/Templates/system_stm32f4xx.c \
 $CUBE/CMSIS/Device/ST/STM32F4xx/Source/Templates/gcc/startup_stm32f427xx.s \
 $CUBE/STM32F4xx_HAL_Driver/Src/stm32f4xx_hal.c \
 $CUBE/STM32F4xx_HAL_Driver/Src/stm32f4xx_hal_cortex.c \
 $CUBE/STM32F4xx_HAL_Driver/Src/stm32f4xx_hal_rcc.c \
 $CUBE/STM32F4xx_HAL_Driver/Src/stm32f4xx_hal_rcc_ex.c \
 $CUBE/STM32F4xx_HAL_Driver/Src/stm32f4xx_hal_gpio.c \
 $CUBE/STM32F4xx_HAL_Driver/Src/stm32f4xx_hal_pwr.c \
 $CUBE/STM32F4xx_HAL_Driver/Src/stm32f4xx_hal_pwr_ex.c"

"$TC/arm-none-eabi-gcc.exe" $CFLAGS $INCLUDES $SOURCES \
    -T fw/lab1/STM32F427VITX_FLASH.ld \
    --specs=nano.specs --specs=nosys.specs \
    -Wl,--gc-sections -Wl,--no-warn-rwx-segments -Wl,-Map="$OUT/lab1.map" -Wl,--print-memory-usage \
    -o "$OUT/lab1.elf"

"$TC/arm-none-eabi-objcopy.exe" -O ihex   "$OUT/lab1.elf" "$OUT/lab1.hex"
"$TC/arm-none-eabi-objcopy.exe" -O binary "$OUT/lab1.elf" "$OUT/lab1.bin"
"$TC/arm-none-eabi-size.exe" "$OUT/lab1.elf"
echo "собрано: $OUT/lab1.elf"
