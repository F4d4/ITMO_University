package functions.log;

import functions.base.LnFunction;

/**
 * Вычисляет log_10(x) = ln(x) / ln(10).
 */
public class Log10Function {

    private final LnFunction lnFunction;

    public Log10Function(LnFunction lnFunction) {
        this.lnFunction = lnFunction;
    }

    public double compute(double x, double epsilon) {
        if (x <= 0) {
            throw new ArithmeticException("log10 is undefined for x <= 0");
        }
        return lnFunction.compute(x, epsilon) / lnFunction.compute(10.0, epsilon);
    }
}
