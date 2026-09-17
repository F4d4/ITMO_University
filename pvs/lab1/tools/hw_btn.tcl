proc rd {addr} { return [expr {[lindex [read_memory $addr 32 1] 0] + 0}] }

proc btn_check {duration_ms} {
    # Сбрасываем накопленные флаги причины сброса (RCC_CSR.RMVF)
    write_memory 0x40023874 32 [list 0x01000000]
    echo "флаги причины сброса очищены"

    set t0 [clock milliseconds]
    set last ""
    while {1} {
        set now [expr {[clock milliseconds] - $t0}]
        if {$now >= $duration_ms} break

        set idr  [rd 0x40020810]
        set odr  [rd 0x40020C14]
        set tick [rd 0x20000134]
        set w1   [rd 0x200000e4]
        set btn  [expr {($idr >> 15) & 1}]
        set cnt  [expr {($w1 >> 8) & 0xff}]

        set s "$btn$cnt$odr"
        if {$s ne $last} {
            echo [format "%6d  PC15=%d  tick=%6d  счётчик=%2d  ODR=0x%04x" $now $btn $tick $cnt $odr]
            set last $s
        }
    }
    set csr [rd 0x40023874]
    echo [format "RCC_CSR = 0x%08x" $csr]
    echo [format "  сброс по выводу NRST (PINRSTF) : %d" [expr {($csr >> 26) & 1}]]
    echo [format "  просадка питания    (BORRSTF) : %d" [expr {($csr >> 25) & 1}]]
    echo [format "  программный сброс   (SFTRSTF) : %d" [expr {($csr >> 28) & 1}]]
    echo [format "  сторожевой таймер   (IWDGRSTF): %d" [expr {($csr >> 29) & 1}]]
    echo "--- конец ---"
}
