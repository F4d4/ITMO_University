package org.example;

public enum TracePoint {
    ENTER_SORT,          // вход в sort
    BASE_CASE,           // базовый случай
    SPLIT,               // разбиение
    ENTER_MERGE,         // вход в merge
    COMPARE,             // сравнение элементов
    TAKE_LEFT,           // взяли из левой части
    TAKE_RIGHT,          // взяли из правой части
    DRAIN_LEFT,          // добрали остаток слева
    DRAIN_RIGHT,         // добрали остаток справа
    WRITE_BACK           // запись результата в исходный массив
}