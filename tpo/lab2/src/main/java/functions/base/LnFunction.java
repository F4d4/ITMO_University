package functions.base;

/**
 * Вычисляет ln(x) через ряд: ln(x) = 2 * (t + t^3/3 + t^5/5 + ...), t = (x-1)/(x+1).
 * Использует приведение диапазона к [0.5, 2] для быстрой сходимости: ln(x) = k*ln(2) + ln(x/2^k).
 * Это базовая функция, реализованная с нуля без использования Math.log().
 */
public class LnFunction {

    public double compute(double x, double epsilon) {
        if (x <= 0) {
            throw new ArithmeticException("Logarithm is undefined for x <= 0");
        }

        // Приводим x к [0.5, 2], чтобы t = (x-1)/(x+1) оставался в [-1/3, 1/3] для быстрой сходимости
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
