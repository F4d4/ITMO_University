#!/bin/sh
# Прошивка стенда SDK-1.1M через встроенный отладчик FT2232 и OpenOCD.
#
# Аргументы:
#   $1 - конфигурация сборки: hal (по умолчанию) или myuart;
#   $2 - "erase" - перед записью полностью стереть флеш-память (оба банка),
#        чтобы на стенде не осталось чужих программ.
#
# Подключение выполняется с удержанием сброса (connect_assert_srst в
# конфигурации отладчика), а перед снятием сброса взводится перехват вектора
# сброса (DEMCR.VC_CORERESET): ядро останавливается, не выполнив ни одной
# команды старой программы. Иначе к стенду с испорченной или пустой
# флеш-памятью подключиться нельзя.
set -e
ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"

CONFIG=${1:-hal}
ELF_WIN=$(cygpath -m "$ROOT/fw/build/$CONFIG/lab2.elf")
OOCD="/c/Users/verdi/AppData/Local/Microsoft/WinGet/Packages/xpack-dev-tools.openocd-xpack_Microsoft.Winget.Source_8wekyb3d8bbwe/xpack-openocd-0.12.0-7"

ERASE_ALL=""
if [ "$2" = "erase" ]; then
    ERASE_ALL="stm32f4x mass_erase 0"
fi

"$OOCD/bin/openocd.exe" \
    -s "fw/openocd" -s "$OOCD/openocd/scripts" \
    -f "SDK1_1_M FTDBG.cfg" \
    -c "init" \
    -c "mww 0xE000EDFC 0x01000001" \
    -c "adapter deassert srst" \
    -c "halt" \
    -c "$ERASE_ALL" \
    -c "flash write_image erase \"$ELF_WIN\"" \
    -c "verify_image \"$ELF_WIN\"" \
    -c "mww 0xE000EDFC 0x00000000" \
    -c "reset run" \
    -c "shutdown"
