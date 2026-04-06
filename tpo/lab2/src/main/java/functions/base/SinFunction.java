package functions.base;

/**
 * Computes sin(x) via Taylor series: sin(x) = x - x^3/3! + x^5/5! - ...
 * This is a base function implemented from scratch without using Math.sin().
 */
public class SinFunction {

    public double compute(double x, double epsilon) {
        // Normalize to [-2π, 2π] to reduce floating-point cancellation
        x = x % (2 * Math.PI);

        double result = 0;
        double term = x;
        int n = 1;

        while (Math.abs(term) >= epsilon) {
            result += term;
            term *= -(x * x) / ((2.0 * n) * (2.0 * n + 1));
            n++;
        }

        return result;
    }
}
