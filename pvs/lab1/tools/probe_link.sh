#!/bin/sh
# Проверка связи со стендом без прошивки.
# Читает регистр DBGMCU_IDCODE: младшие 12 бит должны дать 0x419,
# что соответствует семейству STM32F42x/F43x.
set -e
ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"
OOCD="/c/Users/verdi/AppData/Local/Microsoft/WinGet/Packages/xpack-dev-tools.openocd-xpack_Microsoft.Winget.Source_8wekyb3d8bbwe/xpack-openocd-0.12.0-7"

"$OOCD/bin/openocd.exe" \
    -s "fw/openocd" -s "$OOCD/openocd/scripts" \
    -f "SDK1_1_M FTDBG.cfg" \
    -c "init; targets; mdw 0xE0042000; shutdown"
