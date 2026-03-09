package org.example;

public record TraceEvent(TracePoint point, int a, int b, int c) {
    // a,b,c — параметры (например l,m,r или i,j,-1)
}