package functions.base;

/**
 * Computes ln(x) via the series: ln(x) = 2 * (t + t^3/3 + t^5/5 + ...), t = (x-1)/(x+1).
 * The series converges for all x > 0. Implemented from scratch without Math.log().
 */
public class LnFunction {

    public double compute(double x, double epsilon) {
        if (x <= 0) {
            throw new ArithmeticException("Logarithm is undefined for x <= 0");
        }

        double t = (x - 1.0) / (x + 1.0);
        double t2 = t * t;
        double result = 0;
        double term = t;
        int n = 1;

        while (Math.abs(term / n) >= epsilon) {
            result += term / n;
            n += 2;
            term *= t2;
        }

        return 2 * result;
    }
}
