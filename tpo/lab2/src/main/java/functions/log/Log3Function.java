package functions.log;

import functions.base.LnFunction;

/**
 * Вычисляет log_3(x) = ln(x) / ln(3).
 */
public class Log3Function {

    private final LnFunction lnFunction;

    public Log3Function(LnFunction lnFunction) {
        this.lnFunction = lnFunction;
    }

    public double compute(double x, double epsilon) {
        if (x <= 0) {
            throw new ArithmeticException("log3 is undefined for x <= 0");
        }
        return lnFunction.compute(x, epsilon) / lnFunction.compute(3.0, epsilon);
    }
}
