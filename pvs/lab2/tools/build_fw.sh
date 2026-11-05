#!/bin/sh
# Сборка прошивки для стенда SDK-1.1M (STM32F427VIT6), лабораторная работа 2.
#
# Аргумент: конфигурация сборки
#   hal    - драйверы UART на основе стандартного драйвера HAL (часть 1, по умолчанию)
#   myuart - собственный регистровый драйвер USART (часть 2, способ 1)
set -e

ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"

CONFIG=${1:-hal}
TC="/c/Program Files (x86)/Arm GNU Toolchain arm-none-eabi/14.2 rel1/bin"
# Пакет STM32CubeF4 уже скачан для лабораторной работы 1, повторно не копируется
CUBE=${CUBE:-../lab1/third_party/STM32CubeF4/Drivers}
HAL=$CUBE/STM32F4xx_HAL_Driver/Src

case "$CONFIG" in
    hal)
        BACKEND_DEF=
        BACKEND_SRC="fw/Core/Src/uart_hal.c fw/Core/Src/uart_hal_it.c fw/Core/Src/uart_hal_poll.c \
                     $HAL/stm32f4xx_hal_uart.c $HAL/stm32f4xx_hal_dma.c"
        ;;
    myuart)
        BACKEND_DEF=-DUART_BACKEND_MY
        BACKEND_SRC="fw/Core/Src/myuart.c fw/Core/Src/uart_my.c"
        ;;
    *)
        echo "неизвестная конфигурация: $CONFIG (ожидается hal или myuart)"; exit 2 ;;
esac

OUT=fw/build/$CONFIG
mkdir -p "$OUT"

CFLAGS="-mcpu=cortex-m4 -mthumb -mfpu=fpv4-sp-d16 -mfloat-abi=hard \
 -DUSE_HAL_DRIVER -DSTM32F427xx -DHSE_VALUE=25000000 $BACKEND_DEF \
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
 fw/Core/Src/morse_codec.c \
 fw/Core/Src/morse_buffer.c \
 fw/Core/Src/morse_player.c \
 fw/Core/Src/morse_app.c \
 $BACKEND_SRC \
 $CUBE/CMSIS/Device/ST/STM32F4xx/Source/Templates/system_stm32f4xx.c \
 $CUBE/CMSIS/Device/ST/STM32F4xx/Source/Templates/gcc/startup_stm32f427xx.s \
 $HAL/stm32f4xx_hal.c \
 $HAL/stm32f4xx_hal_cortex.c \
 $HAL/stm32f4xx_hal_rcc.c \
 $HAL/stm32f4xx_hal_rcc_ex.c \
 $HAL/stm32f4xx_hal_gpio.c \
 $HAL/stm32f4xx_hal_pwr.c \
 $HAL/stm32f4xx_hal_pwr_ex.c"

"$TC/arm-none-eabi-gcc.exe" $CFLAGS $INCLUDES $SOURCES \
    -T fw/STM32F427VITX_FLASH.ld \
    --specs=nano.specs --specs=nosys.specs \
    -Wl,--gc-sections -Wl,--no-warn-rwx-segments -Wl,-Map="$OUT/lab2.map" -Wl,--print-memory-usage \
    -o "$OUT/lab2.elf"

"$TC/arm-none-eabi-objcopy.exe" -O ihex   "$OUT/lab2.elf" "$OUT/lab2.hex"
"$TC/arm-none-eabi-objcopy.exe" -O binary "$OUT/lab2.elf" "$OUT/lab2.bin"
"$TC/arm-none-eabi-size.exe" "$OUT/lab2.elf"
echo "собрано: $OUT/lab2.elf"
