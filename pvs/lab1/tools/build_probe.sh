#!/bin/sh
# Сборка пробы полярности индикации стенда
set -e
ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"
TC="/c/Program Files (x86)/Arm GNU Toolchain arm-none-eabi/14.2 rel1/bin"
CUBE=third_party/STM32CubeF4/Drivers
OUT=fw/build/probe
mkdir -p "$OUT"

"$TC/arm-none-eabi-gcc.exe" \
  -mcpu=cortex-m4 -mthumb -mfpu=fpv4-sp-d16 -mfloat-abi=hard \
  -DUSE_HAL_DRIVER -DSTM32F427xx -DHSE_VALUE=25000000 \
  -O2 -g3 -Wall -Wextra -ffunction-sections -fdata-sections -std=gnu11 \
  -Ifw/lab1/Core/Inc \
  -I$CUBE/STM32F4xx_HAL_Driver/Inc \
  -I$CUBE/CMSIS/Device/ST/STM32F4xx/Include \
  -I$CUBE/CMSIS/Include \
  fw/probe/probe_main.c \
  fw/lab1/Core/Src/stm32f4xx_it.c \
  fw/lab1/Core/Src/sys_tick_hal.c \
  fw/lab1/Core/Src/mygpio.c \
  $CUBE/CMSIS/Device/ST/STM32F4xx/Source/Templates/system_stm32f4xx.c \
  $CUBE/CMSIS/Device/ST/STM32F4xx/Source/Templates/gcc/startup_stm32f427xx.s \
  $CUBE/STM32F4xx_HAL_Driver/Src/stm32f4xx_hal.c \
  $CUBE/STM32F4xx_HAL_Driver/Src/stm32f4xx_hal_cortex.c \
  $CUBE/STM32F4xx_HAL_Driver/Src/stm32f4xx_hal_rcc.c \
  $CUBE/STM32F4xx_HAL_Driver/Src/stm32f4xx_hal_rcc_ex.c \
  $CUBE/STM32F4xx_HAL_Driver/Src/stm32f4xx_hal_pwr.c \
  $CUBE/STM32F4xx_HAL_Driver/Src/stm32f4xx_hal_pwr_ex.c \
  -T fw/lab1/STM32F427VITX_FLASH.ld \
  --specs=nano.specs --specs=nosys.specs \
  -Wl,--gc-sections -Wl,--no-warn-rwx-segments -Wl,-Map="$OUT/probe.map" \
  -o "$OUT/probe.elf"

"$TC/arm-none-eabi-size.exe" "$OUT/probe.elf"
echo "собрано: $OUT/probe.elf"
