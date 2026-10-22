proc raw_watch {n} {
    for {set i 0} {$i < $n} {incr i} {
        echo "--- замер $i ---"
        mdw 0x200000bc    ;# b_down, b_counter
        mdw 0x40020810    ;# GPIOC IDR
        mdw 0x40020C14    ;# GPIOD ODR
        mdw 0x20000104    ;# uwTick
    }
    echo "--- конец ---"
}
