package functions.trig;

import functions.base.SinFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Tests for TanFunction using table stubs for sin and cos.
 */
@ExtendWith(MockitoExtension.class)
class TanFunctionTest {

    private static final double EPS   = 1e-9;
    private static final double DELTA = 1e-6;

    @Mock private SinFunction sinStub;
    @Mock private CosFunction cosStub;

    @Test
    void tanQuarterPiIsOne_withStubs() {
        // tan(π/4) = sin(π/4)/cos(π/4) = 1
        double root2over2 = Math.sqrt(2) / 2;
        when(sinStub.compute(eq(Math.PI / 4), anyDouble())).thenReturn(root2over2);
        when(cosStub.compute(eq(Math.PI / 4), anyDouble())).thenReturn(root2over2);
        TanFunction tan = new TanFunction(sinStub, cosStub);

        assertEquals(1.0, tan.compute(Math.PI / 4, EPS), DELTA);
    }

    @Test
    void tanZeroIsZero_withStubs() {
        when(sinStub.compute(eq(0.0), anyDouble())).thenReturn(0.0);
        when(cosStub.compute(eq(0.0), anyDouble())).thenReturn(1.0);
        TanFunction tan = new TanFunction(sinStub, cosStub);

        assertEquals(0.0, tan.compute(0.0, EPS), DELTA);
    }

    @Test
    void tanThrowsWhenCosIsZero_withStubs() {
        when(cosStub.compute(eq(Math.PI / 2), anyDouble())).thenReturn(0.0);
        TanFunction tan = new TanFunction(sinStub, cosStub);

        assertThrows(ArithmeticException.class, () -> tan.compute(Math.PI / 2, EPS));
    }

    @Test
    void tanNegativeAngle_withStubs() {
        // tan(-π/4) = -1
        double root2over2 = Math.sqrt(2) / 2;
        when(sinStub.compute(eq(-Math.PI / 4), anyDouble())).thenReturn(-root2over2);
        when(cosStub.compute(eq(-Math.PI / 4), anyDouble())).thenReturn(root2over2);
        TanFunction tan = new TanFunction(sinStub, cosStub);

        assertEquals(-1.0, tan.compute(-Math.PI / 4, EPS), DELTA);
    }

    @Test
    void tanWithRealFunctions_quarterPi() {
        SinFunction realSin = new SinFunction();
        CosFunction realCos = new CosFunction(realSin);
        TanFunction tan = new TanFunction(realSin, realCos);

        assertEquals(1.0,  tan.compute(Math.PI / 4, EPS), DELTA);
        assertEquals(0.0,  tan.compute(0.0, EPS), DELTA);
        assertEquals(-1.0, tan.compute(-Math.PI / 4, EPS), DELTA);
    }
}
