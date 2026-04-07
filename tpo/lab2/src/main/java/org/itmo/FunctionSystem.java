package org.itmo;

import functions.log.Log10Function;
import functions.log.Log2Function;
import functions.log.Log3Function;
import functions.trig.CotFunction;
import functions.trig.TanFunction;

/**
 * Кусочно-заданная система функций (вариант 28931):
 *   x <= 0 : cot(x) * tan(x)
 *   x >  0 : ((((log2(x) / log10(x))^2 * log3(x)^2)^2) * log2(x)
 */
public class FunctionSystem {

    private final CotFunction cotFunction;
    private final TanFunction tanFunction;
    private final Log2Function log2Function;
    private final Log3Function log3Function;
    private final Log10Function log10Function;

    public FunctionSystem(CotFunction cotFunction, TanFunction tanFunction,
                          Log2Function log2Function, Log3Function log3Function,
                          Log10Function log10Function) {
        this.cotFunction = cotFunction;
        this.tanFunction = tanFunction;
        this.log2Function = log2Function;
        this.log3Function = log3Function;
        this.log10Function = log10Function;
    }

    public double compute(double x, double epsilon) {
        if (x <= 0) {
            return cotFunction.compute(x, epsilon) * tanFunction.compute(x, epsilon);
        } else {
            double log2x  = log2Function.compute(x, epsilon);
            double log10x = log10Function.compute(x, epsilon);
            double log3x  = log3Function.compute(x, epsilon);

            // ((((log2(x) / log10(x))^2) * log3(x)^2)^2) * log2(x)
            double a = log2x / log10x;
            double b = a * a;
            double c = log3x * log3x;
            double d = b * c;
            double e = d * d;
            return e * log2x;
        }
    }
}
