package functions.trig;

import functions.base.SinFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Тесты для CosFunction.
 */
@ExtendWith(MockitoExtension.class)
class CosFunctionTest {

    private static final double DELTA = 1e-9;

    @Mock
    private SinFunction sinStub;


    @Test
    void cosZeroIsOne_withStub() {
        // cos(0) = sin(pi/2) = 1
        when(sinStub.compute(eq(Math.PI / 2), anyDouble())).thenReturn(1.0);
        CosFunction cos = new CosFunction(sinStub);

        assertEquals(1.0, cos.compute(0.0, 1e-9), DELTA);
    }

    @Test
    void cosHalfPiIsZero_withStub() {
        // cos(pi/2) = sin(0) = 0
        when(sinStub.compute(eq(0.0), anyDouble())).thenReturn(0.0);
        CosFunction cos = new CosFunction(sinStub);

        assertEquals(0.0, cos.compute(Math.PI / 2, 1e-9), DELTA);
    }

    @Test
    void cosPiIsMinusOne_withStub() {
        // cos(pi) = sin(pi/2 - pi) = sin(-pi/2) = -1
        when(sinStub.compute(eq(Math.PI / 2 - Math.PI), anyDouble())).thenReturn(-1.0);
        CosFunction cos = new CosFunction(sinStub);

        assertEquals(-1.0, cos.compute(Math.PI, 1e-9), DELTA);
    }
    
    @Test
    void cosWithRealSin_knownValues() {
        SinFunction realSin = new SinFunction();
        CosFunction cos = new CosFunction(realSin);
        double eps = 1e-9;

        assertAll(
                () -> assertEquals(1.0,  cos.compute(0.0,         eps), 1e-6),
                () -> assertEquals(0.0,  cos.compute(Math.PI / 2, eps), 1e-6),
                () -> assertEquals(-1.0, cos.compute(Math.PI,     eps), 1e-6),
                () -> assertEquals(0.5,  cos.compute(Math.PI / 3, eps), 1e-6) // cos(60) = 0.5
        );
    }

    @Test
    void pythagoreanIdentityWithRealSin() {
        SinFunction realSin = new SinFunction();
        CosFunction cos = new CosFunction(realSin);
        double eps = 1e-9;
        double x = 1.2;

        double s = realSin.compute(x, eps);
        double c = cos.compute(x, eps);
        assertEquals(1.0, s * s + c * c, 1e-6);
    }
}
