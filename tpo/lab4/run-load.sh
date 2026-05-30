#!/usr/bin/env bash
# Нагрузочный тест: 3 конфигурации при номинальной нагрузке 100 req/min, 5 потоков, 3 минуты.
# Запускается НА hel (FreeBSD + openjdk17). Целевой сервер виден только из внутренней сети.
set -euo pipefail

export JAVA_HOME=/usr/local/openjdk17
export PATH="$JAVA_HOME/bin:$PATH"
# Критично: ограничить heap, иначе openjdk17 на этой машине падает на резервировании ~30 ГБ.
export JVM_ARGS="-Xms512m -Xmx2g"

JM="$HOME/apache-jmeter-5.6.3/bin/jmeter"
PLAN="$HOME/lab4/load-test.jmx"
OUT="$HOME/lab4/results"
mkdir -p "$OUT"

THROUGHPUT=100   # суммарный req/min (номинал: 5 польз. * 20 req/min)
THREADS=5        # максимум параллельных пользователей по варианту
DURATION=180     # секунд на конфигурацию
RAMPUP=10

for c in 1 2 3; do
  echo ">>> Load test: config=$c  throughput=${THROUGHPUT}/min  threads=${THREADS}  duration=${DURATION}s"
  rm -rf "$OUT/report_cfg${c}"
  rm -f  "$OUT/load_cfg${c}.jtl"
  "$JM" -n -t "$PLAN" \
    -l "$OUT/load_cfg${c}.jtl" \
    -e -o "$OUT/report_cfg${c}" \
    -Jconfig="$c" -Jthroughput="$THROUGHPUT" -Jthreads="$THREADS" \
    -Jduration="$DURATION" -Jrampup="$RAMPUP" \
    -j "$OUT/jmeter_cfg${c}.log"
  echo "<<< done config=$c"
  sleep 15   # пауза для общего учебного сервера
done
echo "ALL LOAD TESTS DONE"
