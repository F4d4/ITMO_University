#!/usr/bin/env bash
# Стресс-тест выбранной конфигурации (по умолчанию cfg2): серия плоских прогонов с растущей
# нагрузкой. Каждый уровень — отдельный JTL = одна точка графика. Запускается НА hel.
# Идемпотентен: уровни с уже существующим непустым JTL пропускаются (можно до-запускать).
set -u

export JAVA_HOME=/usr/local/openjdk17
export PATH="$JAVA_HOME/bin:$PATH"
export JVM_ARGS="-Xms512m -Xmx2g"

JM="$HOME/apache-jmeter-5.6.3/bin/jmeter"
PLAN="$HOME/lab4/load-test.jmx"
OUT="$HOME/lab4/results/stress"
mkdir -p "$OUT"

CONFIG="${1:-2}"   # какую конфигурацию стрессим (аргумент скрипта, по умолчанию 2)
DUR=75             # секунд на уровень
RAMPUP=10

# Лесенка "целевой_throughput:threads". Потоков с запасом: при росте времени отклика R
# closed-model потолок req/s = threads/R падает, поэтому threads растут быстрее target.
levels=("100:5" "200:8" "300:12" "400:16" "500:20" "600:25" \
        "700:30" "800:35" "900:40" "1000:45" "1200:55" "1500:70")

for lv in "${levels[@]}"; do
  tp="${lv%%:*}"; th="${lv##*:}"
  jtl="$OUT/stress_tp${tp}.jtl"
  if [ -s "$jtl" ]; then
    echo "=== skip target=${tp}/min (already have $(basename "$jtl")) ==="
    continue
  fi
  echo ">>> Stress: config=${CONFIG}  target=${tp}/min  threads=${th}  duration=${DUR}s"
  rm -f "$jtl"
  # Только JTL (per-level HTML-дашборды не генерируем: экономия места под квоту;
  # итоговый график строится из объединённого JTL средствами CMDRunner).
  "$JM" -n -t "$PLAN" \
    -l "$jtl" \
    -Jconfig="$CONFIG" -Jthroughput="$tp" -Jthreads="$th" \
    -Jduration="$DUR" -Jrampup="$RAMPUP" \
    -j "$OUT/jmeter_tp${tp}.log" \
    || echo "!!! level ${tp} exited non-zero (continuing)"
  echo "<<< done target=${tp}/min"
  sleep 15   # пауза для общего учебного сервера
done
echo "ALL STRESS LEVELS DONE"
