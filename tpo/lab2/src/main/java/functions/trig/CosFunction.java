package functions.trig;

import functions.base.SinFunction;

/**
 * Computes cos(x) using the co-function identity: cos(x) = sin(π/2 - x).
 * Depends on SinFunction as the underlying base implementation.
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
