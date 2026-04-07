package functions.base;

/**
 * Вычисляет sin(x) через ряд Тейлора: sin(x) = x - x^3/3! + x^5/5! - ...
 * Это базовая функция, реализованная с нуля без использования Math.sin().
 */
public class SinFunction {

    public double compute(double x, double epsilon) {
        // Нормализация к [-2π, 2π] для уменьшения потерь точности
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
