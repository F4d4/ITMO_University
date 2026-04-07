package functions.base;

/**
 * Вычисляет ln(x) с помощью ряда: ln(x) = 2 * (t + t^3/3 + t^5/5 + ...), t = (x-1)/(x+1).
 * Ряд сходится для всех x > 0. Реализовано с нуля без использования Math.log().
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
