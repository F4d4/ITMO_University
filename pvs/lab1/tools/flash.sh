#!/bin/sh
# Прошивка стенда SDK-1.1M через встроенный отладчик FT2232 и OpenOCD.
# Аргумент: путь к .elf (по умолчанию конфигурация mygpio).
#
# Последовательность отличается от штатной команды program и объясняется так.
# Подключение выполняется с удержанием сигнала сброса (connect_assert_srst в
# конфигурации отладчика), иначе к стенду с пустой флеш-памятью подключиться
# нельзя: ядро выбирает из таблицы векторов значение 0xFFFFFFFF, уходит в
# состояние lockup, и загрузчик программирования во внутреннее ОЗУ уже не
# записывается. Поэтому перед снятием сброса взводится перехват вектора
# сброса (DEMCR.VC_CORERESET), и ядро останавливается, не выполнив ни одной
# команды.
set -e
ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"

ELF=${1:-fw/build/mygpio/lab1.elf}
ELF_WIN=$(cygpath -m "$ROOT/$ELF")
OOCD="/c/Users/verdi/AppData/Local/Microsoft/WinGet/Packages/xpack-dev-tools.openocd-xpack_Microsoft.Winget.Source_8wekyb3d8bbwe/xpack-openocd-0.12.0-7"

"$OOCD/bin/openocd.exe" \
    -s "fw/openocd" -s "$OOCD/openocd/scripts" \
    -f "SDK1_1_M FTDBG.cfg" \
    -c "init" \
    -c "mww 0xE000EDFC 0x01000001" \
    -c "adapter deassert srst" \
    -c "halt" \
    -c "flash write_image erase \"$ELF_WIN\"" \
    -c "verify_image \"$ELF_WIN\"" \
    -c "reset run" \
    -c "shutdown"
