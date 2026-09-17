# Наблюдение за внутренним состоянием драйверов на работающем стенде.
#
# Переменные читаются 32-разрядными обращениями по выровненным адресам.
# Результат read_memory приводится к числу прибавлением нуля: встроенный
# интерпретатор возвращает строку вида 0x1234, и подстановка её напрямую
# в format %d даёт ноль вместо значения.
proc rd {addr} {
    return [expr {[lindex [read_memory $addr 32 1] 0] + 0}]
}

proc hw_debug {duration_ms} {
    set t0 [clock milliseconds]
    set last ""
    set forced 3
    while {1} {
        set now [expr {[clock milliseconds] - $t0}]
        if {$now >= $duration_ms} break

        set w1   [rd 0x200000e4]   ;# b_down, b_counter, s_count
        set w2   [rd 0x200000a0]   ;# b_tail, b_head
        set w3   [rd 0x20000130]   ;# b_state
        set idr  [rd 0x40020810]
        set odr  [rd 0x40020C14]
        set tick [rd 0x20000134]

        set down  [expr {$w1 & 0xff}]
        set cnt   [expr {($w1 >> 8) & 0xff}]
        set tail  [expr {$w2 & 0xff}]
        set head  [expr {($w2 >> 8) & 0xff}]
        set state [expr {($w3 >> 8) & 0xff}]
        set btn   [expr {($idr >> 15) & 1}]

        set s "$cnt$down$head$tail$state$btn$odr"
        if {$s ne $last || $forced > 0} {
            echo [format "%6d  tick=%6d  PC15=%d  счётчик=%2d  нажата=%d  очередь=%d/%d  сост=%d  ODR=0x%04x" \
                  $now $tick $btn $cnt $down $head $tail $state $odr]
            set last $s
            if {$forced > 0} { incr forced -1 }
        }
    }
    echo "--- конец ---"
}
