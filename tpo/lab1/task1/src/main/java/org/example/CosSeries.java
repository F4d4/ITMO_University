package org.example;

public final class CosSeries {
    private static final double TWO_PI = 2.0 * Math.PI;

    private CosSeries() {}

    public static double cos(double x, double eps, int maxTerms) {
        if (Double.isNaN(x) || Double.isInfinite(x)) return Double.NaN;
        if (!(eps > 0.0)) throw new IllegalArgumentException("eps > 0");
        if (maxTerms <= 0) throw new IllegalArgumentException("maxTerms > 0");

        double xr = reduceToMinusPiPi(x); // уменьшаем x

        double sum = 1.0;
        double term = 1.0;

        for (int k = 1; k < maxTerms; k++) {
            double denom = (2.0 * k - 1.0) * (2.0 * k);
            term *= -(xr * xr) / denom;
            sum += term;

            if (Math.abs(term) <= eps) break; // критерий остановки
        }

        return sum;
    }

    public static double cos(double x) {
        return cos(x, 1e-12, 20_000);
    }

    private static double reduceToMinusPiPi(double x) {
        double r = x % TWO_PI;
        if (r > Math.PI) r -= TWO_PI;
        if (r < -Math.PI) r += TWO_PI;
        return r;
    }
}