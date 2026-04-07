package functions.base;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Модульные тесты для LnFunction (реализация через разложение в ряд).
 */
class LnFunctionTest {

    private static final double EPSILON = 1e-9;
    private static final double DELTA   = 1e-6;

    private LnFunction ln;

    @BeforeEach
    void setUp() {
        ln = new LnFunction();
    }

    @Test
    void lnOfOneIsZero() {
        assertEquals(0.0, ln.compute(1.0, EPSILON), DELTA);
    }

    @Test
    void lnOfEIsOne() {
        assertEquals(1.0, ln.compute(Math.E, EPSILON), DELTA);
    }

    @Test
    void lnOfESquaredIsTwo() {
        assertEquals(2.0, ln.compute(Math.E * Math.E, EPSILON), DELTA);
    }

    @Test
    void lnOfTwoIsCorrect() {
        assertEquals(Math.log(2), ln.compute(2.0, EPSILON), DELTA);
    }

    @Test
    void lnOfTenIsCorrect() {
        assertEquals(Math.log(10), ln.compute(10.0, EPSILON), DELTA);
    }

    @Test
    void lnOfFractionIsNegative() {
        // ln(0.5) = -ln(2)
        assertEquals(-Math.log(2), ln.compute(0.5, EPSILON), DELTA);
    }

    @Test
    void lnOfZeroThrows() {
        assertThrows(ArithmeticException.class, () -> ln.compute(0.0, EPSILON));
    }

    @Test
    void lnOfNegativeThrows() {
        assertThrows(ArithmeticException.class, () -> ln.compute(-1.0, EPSILON));
    }

    @Test
    void lnSatisfiesLogarithmProperty() {
        // ln(a*b) = ln(a) + ln(b)
        double a = 3.0, b = 5.0;
        assertEquals(ln.compute(a, EPSILON) + ln.compute(b, EPSILON),
                     ln.compute(a * b, EPSILON), DELTA);
    }
}
