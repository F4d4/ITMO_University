package functions.base;

/**
 * Computes ln(x) via the series: ln(x) = 2 * (t + t^3/3 + t^5/5 + ...), t = (x-1)/(x+1).
 * Uses range reduction to [0.5, 2] for fast convergence: ln(x) = k*ln(2) + ln(x/2^k).
 * This is a base function implemented from scratch without using Math.log().
 */
public class LnFunction {

    public double compute(double x, double epsilon) {
        if (x <= 0) {
            throw new ArithmeticException("Logarithm is undefined for x <= 0");
        }

        // Reduce x to [0.5, 2] so t = (x-1)/(x+1) stays in [-1/3, 1/3] for fast convergence
        int k = 0;
        while (x > 2.0) { x /= 2.0; k++; }
        while (x < 0.5) { x *= 2.0; k--; }

        return series(x, epsilon) + k * series(2.0, epsilon);
    }

    private double series(double x, double epsilon) {
        double t = (x - 1.0) / (x + 1.0);
        double t2 = t * t;
        double result = 0;
        double term = t;
        int n = 1;

        while (Math.abs(term) >= epsilon) {
            result += term / n;
            n += 2;
            term *= t2;
        }

        return 2 * result;
    }
}
