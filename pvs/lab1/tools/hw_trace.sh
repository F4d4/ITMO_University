#!/bin/sh
# Съём реального поведения стенда. Аргумент: длительность в миллисекундах.
set -e
ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"
MS=${1:-20000}
OOCD="/c/Users/verdi/AppData/Local/Microsoft/WinGet/Packages/xpack-dev-tools.openocd-xpack_Microsoft.Winget.Source_8wekyb3d8bbwe/xpack-openocd-0.12.0-7"

"$OOCD/bin/openocd.exe" \
    -s "fw/openocd" -s "$OOCD/openocd/scripts" \
    -f "SDK1_1_M FTDBG.cfg" \
    -c "reset_config connect_deassert_srst" \
    -c "init" \
    -c "source [pwd]/tools/hw_trace.tcl" \
    -c "hw_trace $MS" \
    -c "shutdown"
