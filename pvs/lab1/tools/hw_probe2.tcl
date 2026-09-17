proc catch_press {duration_ms} {
    set t0 [clock milliseconds]
    set reported 0
    while {[expr {[clock milliseconds] - $t0}] < $duration_ms} {
        set idr [lindex [read_memory 0x40020810 32 1] 0]
        set btn [expr {($idr >> 15) & 1}]
        if {$btn == 0 && $reported < 6} {
            set now [expr {[clock milliseconds] - $t0}]
            set s ""
            for {set k 0} {$k < 5} {incr k} {
                set w [lindex [read_memory 0x200000e4 32 1] 0]
                set i2 [lindex [read_memory 0x40020810 32 1] 0]
                append s [format " cnt=%d/idr15=%d" [expr {($w >> 8) & 0xff}] [expr {($i2 >> 15) & 1}]]
            }
            set tick [lindex [read_memory 0x20000134 32 1] 0]
            echo [format "%6d ЗАМКНУТА  tick=%d %s" $now $tick $s]
            incr reported
        }
    }
    echo "--- конец ---"
}
