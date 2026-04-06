package functions.trig;

import functions.base.SinFunction;

/**
 * Computes cot(x) = cos(x) / sin(x).
 */
public class CotFunction {

    private final SinFunction sinFunction;
    private final CosFunction cosFunction;

    public CotFunction(SinFunction sinFunction, CosFunction cosFunction) {
        this.sinFunction = sinFunction;
        this.cosFunction = cosFunction;
    }

    public double compute(double x, double epsilon) {
        double sin = sinFunction.compute(x, epsilon);
        if (Math.abs(sin) < epsilon) {
            throw new ArithmeticException("cot(x) is undefined: sin(x) = 0 at x = " + x);
        }
        return cosFunction.compute(x, epsilon) / sin;
    }
}
