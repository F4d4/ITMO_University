package org.example;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CosSeriesTest {

    private static final double EPS = 1e-12;
    private static final int MAX_TERMS = 20_000;

    static Stream<double[]> keyPoints() {
        double p = Math.PI;
        double e = 1e-6;

        return Stream.of(
                new double[]{0.0, 1.0},
                new double[]{p / 2.0, 0.0},
                new double[]{p, -1.0},
                new double[]{3.0 * p / 2.0, 0.0},
                new double[]{2.0 * p, 1.0},

                new double[]{(p / 2.0) - e, Math.cos((p / 2.0) - e)},
                new double[]{(p / 2.0) + e, Math.cos((p / 2.0) + e)},
                new double[]{p - e, Math.cos(p - e)},
                new double[]{p + e, Math.cos(p + e)},
                new double[]{e, Math.cos(e)},
                new double[]{-e, Math.cos(-e)},

                new double[]{1e-8, Math.cos(1e-8)},
                new double[]{-1e-8, Math.cos(-1e-8)},
                new double[]{1.0, Math.cos(1.0)},
                new double[]{-1.0, Math.cos(-1.0)},
                new double[]{2.0, Math.cos(2.0)},
                new double[]{-2.0, Math.cos(-2.0)},

                new double[]{100.0 * p, Math.cos(100.0 * p)},
                new double[]{1e6, Math.cos(1e6)}
        );
    }

    @ParameterizedTest
    @MethodSource("keyPoints")
    void testKeyPoints(double[] in) {
        double x = in[0];
        double expected = in[1];
        double actual = CosSeries.cos(x, EPS, MAX_TERMS);
        assertEquals(expected, actual, 1e-10);
    }

    @Test
    void testNaNAndInfinity() {
        assertTrue(Double.isNaN(CosSeries.cos(Double.NaN, EPS, MAX_TERMS)));
        assertTrue(Double.isNaN(CosSeries.cos(Double.POSITIVE_INFINITY, EPS, MAX_TERMS)));
        assertTrue(Double.isNaN(CosSeries.cos(Double.NEGATIVE_INFINITY, EPS, MAX_TERMS)));
    }

    @Test
    void testBadParams() {
        assertThrows(IllegalArgumentException.class, () -> CosSeries.cos(1.0, 0.0, MAX_TERMS));
        assertThrows(IllegalArgumentException.class, () -> CosSeries.cos(1.0, -1e-3, MAX_TERMS));
        assertThrows(IllegalArgumentException.class, () -> CosSeries.cos(1.0, EPS, 0));
        assertThrows(IllegalArgumentException.class, () -> CosSeries.cos(1.0, EPS, -10));
    }

    @Test
    void testGridComparison() {
        double from = -2.0 * Math.PI;
        double to = 2.0 * Math.PI;
        double step = 0.1;

        for (double x = from; x <= to; x += step) {
            double expected = Math.cos(x);
            double actual = CosSeries.cos(x, EPS, MAX_TERMS);
            assertEquals(expected, actual, 1e-10, "x=" + x);
        }
    }

    @RepeatedTest(2000)
    void propertyEvenness() {
        Random r = new Random(12345L);
        double x = (r.nextDouble() * 2.0 - 1.0) * 1e4;

        double a = CosSeries.cos(x, EPS, MAX_TERMS);
        double b = CosSeries.cos(-x, EPS, MAX_TERMS);
        assertEquals(a, b, 1e-10);
    }

    @RepeatedTest(2000)
    void propertyPeriodicity() {
        Random r = new Random(54321L);
        double x = (r.nextDouble() * 2.0 - 1.0) * 1e4;
        int k = 1 + r.nextInt(5);

        double a = CosSeries.cos(x, EPS, MAX_TERMS);
        double b = CosSeries.cos(x + 2.0 * Math.PI * k, EPS, MAX_TERMS);
        assertEquals(a, b, 1e-10);
    }

    @RepeatedTest(2000)
    void propertyBounds() {
        Random r = new Random(777L);
        double x = (r.nextDouble() * 2.0 - 1.0) * 1e6;

        double y = CosSeries.cos(x, EPS, MAX_TERMS);
        assertTrue(y >= -1.0000000001 && y <= 1.0000000001);
    }
}