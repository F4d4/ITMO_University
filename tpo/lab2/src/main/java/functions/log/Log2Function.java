package functions.log;

import functions.base.LnFunction;

/**
 * Вычисляет log_2(x) = ln(x) / ln(2).
 */
public class Log2Function {

    private final LnFunction lnFunction;

    public Log2Function(LnFunction lnFunction) {
        this.lnFunction = lnFunction;
    }

    public double compute(double x, double epsilon) {
        if (x <= 0) {
            throw new ArithmeticException("log2 is undefined for x <= 0");
        }
        return lnFunction.compute(x, epsilon) / lnFunction.compute(2.0, epsilon);
    }
}
