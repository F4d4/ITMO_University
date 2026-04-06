package functions.trig;

import functions.base.SinFunction;

/**
 * Computes tan(x) = sin(x) / cos(x).
 */
public class TanFunction {

    private final SinFunction sinFunction;
    private final CosFunction cosFunction;

    public TanFunction(SinFunction sinFunction, CosFunction cosFunction) {
        this.sinFunction = sinFunction;
        this.cosFunction = cosFunction;
    }

    public double compute(double x, double epsilon) {
        double cos = cosFunction.compute(x, epsilon);
        if (Math.abs(cos) < epsilon) {
            throw new ArithmeticException("tan(x) is undefined: cos(x) = 0 at x = " + x);
        }
        return sinFunction.compute(x, epsilon) / cos;
    }
}
