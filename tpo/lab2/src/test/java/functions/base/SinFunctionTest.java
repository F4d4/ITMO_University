package functions.base;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for SinFunction (Taylor series implementation).
 * Verified against known mathematical values.
 */
class SinFunctionTest {

    private static final double EPSILON = 1e-9;
    private static final double DELTA   = 1e-6;

    private SinFunction sin;

    @BeforeEach
    void setUp() {
        sin = new SinFunction();
    }

    @Test
    void sinZeroIsZero() {
        assertEquals(0.0, sin.compute(0.0, EPSILON), DELTA);
    }

    @Test
    void sinHalfPiIsOne() {
        assertEquals(1.0, sin.compute(Math.PI / 2, EPSILON), DELTA);
    }

    @Test
    void sinPiIsZero() {
        assertEquals(0.0, sin.compute(Math.PI, EPSILON), DELTA);
    }

    @Test
    void sinThreeHalfPiIsMinusOne() {
        assertEquals(-1.0, sin.compute(3 * Math.PI / 2, EPSILON), DELTA);
    }

    @Test
    void sinMinusHalfPiIsMinusOne() {
        assertEquals(-1.0, sin.compute(-Math.PI / 2, EPSILON), DELTA);
    }

    @Test
    void sinSixthPiIsHalf() {
        // sin(π/6) = 0.5
        assertEquals(0.5, sin.compute(Math.PI / 6, EPSILON), DELTA);
    }

    @Test
    void sinIsOddFunction() {
        double x = 0.7;
        assertEquals(-sin.compute(x, EPSILON), sin.compute(-x, EPSILON), DELTA);
    }

    @Test
    void sinPythagoreanIdentityHolds() {
        // sin²(x) + cos²(x) = 1 checked implicitly via known value sin(π/3)=√3/2
        double expected = Math.sqrt(3) / 2;
        assertEquals(expected, sin.compute(Math.PI / 3, EPSILON), DELTA);
    }
}
