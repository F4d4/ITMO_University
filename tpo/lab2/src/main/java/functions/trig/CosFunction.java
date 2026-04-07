package functions.trig;

import functions.base.SinFunction;

/**
 * Вычисляет cos(x) с использованием кофункционального тождества: cos(x) = sin(pi/2 - x).
 * Зависит от SinFunction как от базовой реализации.
 */
public class CosFunction {

    private final SinFunction sinFunction;

    public CosFunction(SinFunction sinFunction) {
        this.sinFunction = sinFunction;
    }

    public double compute(double x, double epsilon) {
        return sinFunction.compute(Math.PI / 2 - x, epsilon);
    }
}
