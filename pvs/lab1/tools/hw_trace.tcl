# Съём состояния выводов с работающего стенда через отладочный интерфейс.
# Ядро не останавливается: чтение памяти идёт по шине AHB-AP параллельно
# исполнению программы, поэтому измерение не влияет на её работу.

proc hw_trace {duration_ms} {
    set gpiod_odr 0x40020C14
    set gpioc_idr 0x40020810

    set t0   [clock milliseconds]
    set last ""

    while {1} {
        set now [expr {[clock milliseconds] - $t0}]
        if {$now >= $duration_ms} break

        set odr [lindex [read_memory $gpiod_odr 32 1] 0]
        set idr [lindex [read_memory $gpioc_idr 32 1] 0]

        set green  [expr {($odr >> 13) & 1}]
        set pd14   [expr {($odr >> 14) & 1}]
        set pd15   [expr {($odr >> 15) & 1}]
        set button [expr {($idr >> 15) & 1}]

        set state "$green$pd14$pd15$button"
        if {$state ne $last} {
            if {$pd14 && !$pd15} {
                set color "ЖЁЛТЫЙ "
            } elseif {!$pd14 && $pd15} {
                set color "КРАСНЫЙ"
            } else {
                set color "-      "
            }
            echo [format "%7d  зелёный=%d  двухцветный=%s  кнопка=%s" \
                  $now $green $color [expr {$button ? "отпущена" : "НАЖАТА  "}]]
            set last $state
        }
    }
    echo "--- съём завершён ---"
}
