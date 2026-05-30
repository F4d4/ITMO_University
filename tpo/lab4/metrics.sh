#!/usr/bin/env bash
# Подсчёт метрик времени отклика из JTL-файлов JMeter без Python — только awk/sort/sed
# (портативно для FreeBSD one-true-awk). Исключает окно прогрева WARM секунд
# (первый запрос каждого прогона — холодный старт сервера) и считает тайминги
# по успешным (HTTP 200) сэмплам; error% — по всем сэмплам после прогрева.
#
# Колонки JTL: $1=timeStamp $2=elapsed $4=responseCode $8=success
#
# Использование:
#   bash metrics.sh load           # таблица по load_cfg1/2/3
#   bash metrics.sh stress         # таблица по results/stress/stress_tp*.jtl
#   bash metrics.sh file <jtl>     # одна строка по конкретному файлу
set -u
WARM=${WARM:-15}
RES="${RES:-$HOME/lab4/results}"
TMP="/tmp/_metrics.$$"

pctl() { # <sorted_file> <n> <p>  → значение перцентиля p (0..1)
  local f=$1 n=$2 p=$3 idx
  [ "$n" -eq 0 ] && { echo "NA"; return; }
  idx=$(awk -v n="$n" -v p="$p" 'BEGIN{i=int(p*n+0.999); if(i<1)i=1; if(i>n)i=n; print i}')
  sed -n "${idx}p" "$f"
}

row() { # <jtl> <label> [extra columns...]
  local jtl=$1 label=$2; shift 2
  local start cut n total err avg max endts span rpm errp
  start=$(awk -F, '$1+0>0{print $1}' "$jtl" | sort -n | head -1)
  cut=$(( start + WARM*1000 ))
  awk -F, -v c="$cut" '$1+0>=c && $8=="true"{print $2}' "$jtl" | sort -n > "$TMP"
  n=$(wc -l < "$TMP" | tr -d ' ')
  total=$(awk -F, -v c="$cut" '$1+0>=c{t++} END{print t+0}' "$jtl")
  err=$(awk -F, -v c="$cut" '$1+0>=c && $8!="true"{e++} END{print e+0}' "$jtl")
  endts=$(awk -F, -v c="$cut" '$1+0>=c{print $1}' "$jtl" | sort -n | tail -1)
  span=$(awk -v a="$cut" -v b="$endts" 'BEGIN{printf "%.3f",(b-a)/1000}')
  avg=$(awk '{s+=$1} END{if(NR) printf "%.0f",s/NR; else print "NA"}' "$TMP")
  max=$([ "$n" -gt 0 ] && tail -1 "$TMP" || echo NA)
  rpm=$(awk -v t="$total" -v s="$span" 'BEGIN{if(s>0)printf "%.0f",t/s*60; else print "NA"}')
  errp=$(awk -v e="$err" -v t="$total" 'BEGIN{if(t)printf "%.1f",e/t*100; else print "NA"}')
  printf "%-12s %5s %7s %6s %6s %6s %6s %6s %6s\n" \
    "$label" "${1:-$total}" "$rpm" "$avg" \
    "$(pctl "$TMP" "$n" 0.90)" "$(pctl "$TMP" "$n" 0.95)" \
    "$(pctl "$TMP" "$n" 0.99)" "$max" "$errp"
}

header() { printf "%-12s %5s %7s %6s %6s %6s %6s %6s %6s\n" "$1" "$2" "rpm" "avg" "P90" "P95" "P99" "max" "err%"; }

case "${1:-load}" in
  load)
    header "config" "N"
    for c in 1 2 3; do row "$RES/load_cfg${c}.jtl" "cfg$c"; done ;;
  stress)
    # карта целевой_tp -> threads (как в run-stress.sh), без bash4-массивов
    threads_for() { case "$1" in
      100) echo 5;; 200) echo 8;; 300) echo 12;; 400) echo 16;; 500) echo 20;;
      600) echo 25;; 700) echo 30;; 800) echo 35;; 900) echo 40;; 1000) echo 45;;
      1200) echo 55;; 1500) echo 70;; *) echo '?';; esac; }
    header "target/min" "thr"
    for jtl in $(ls "$RES"/stress/stress_tp*.jtl 2>/dev/null | sed -E 's/.*tp([0-9]+)\.jtl/\1 &/' | sort -n | awk '{print $2}'); do
      tp=$(basename "$jtl" | sed -E 's/.*tp([0-9]+)\.jtl/\1/')
      row "$jtl" "$tp" "$(threads_for "$tp")"
    done ;;
  file)
    header "file" "N"; row "$2" "$(basename "$2")" ;;
  *) echo "usage: metrics.sh [load|stress|file <jtl>]"; exit 1 ;;
esac
rm -f "$TMP"
