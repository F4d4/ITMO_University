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
 * Тесты для CotFunction с использованием табличных заглушек для sin и cos.
 */
@ExtendWith(MockitoExtension.class)
class CotFunctionTest {

    private static final double EPS   = 1e-9;
    private static final double DELTA = 1e-6;

    @Mock private SinFunction sinStub;
    @Mock private CosFunction cosStub;

    @Test
    void cotQuarterPiIsOne_withStubs() {
        // cot(pi/4) = cos(pi/4)/sin(pi/4) = 1
        double root2over2 = Math.sqrt(2) / 2;
        when(sinStub.compute(eq(Math.PI / 4), anyDouble())).thenReturn(root2over2);
        when(cosStub.compute(eq(Math.PI / 4), anyDouble())).thenReturn(root2over2);
        CotFunction cot = new CotFunction(sinStub, cosStub);

        assertEquals(1.0, cot.compute(Math.PI / 4, EPS), DELTA);
    }

    @Test
    void cotHalfPiIsZero_withStubs() {
        // cot(pi/2) = cos(pi/2)/sin(pi/2) = 0/1 = 0
        when(sinStub.compute(eq(Math.PI / 2), anyDouble())).thenReturn(1.0);
        when(cosStub.compute(eq(Math.PI / 2), anyDouble())).thenReturn(0.0);
        CotFunction cot = new CotFunction(sinStub, cosStub);

        assertEquals(0.0, cot.compute(Math.PI / 2, EPS), DELTA);
    }

    @Test
    void cotThrowsWhenSinIsZero_withStubs() {
        when(sinStub.compute(eq(0.0), anyDouble())).thenReturn(0.0);
        CotFunction cot = new CotFunction(sinStub, cosStub);

        assertThrows(ArithmeticException.class, () -> cot.compute(0.0, EPS));
    }

    @Test
    void cotAndTanAreReciprocal_withStubs() {
        // cot(x) * tan(x) = 1
        double root2over2 = Math.sqrt(2) / 2;
        when(sinStub.compute(eq(Math.PI / 4), anyDouble())).thenReturn(root2over2);
        when(cosStub.compute(eq(Math.PI / 4), anyDouble())).thenReturn(root2over2);
        CotFunction cot = new CotFunction(sinStub, cosStub);
        TanFunction tan = new TanFunction(sinStub, cosStub);

        assertEquals(1.0, cot.compute(Math.PI / 4, EPS) * tan.compute(Math.PI / 4, EPS), DELTA);
    }

    @Test
    void cotWithRealFunctions() {
        SinFunction realSin = new SinFunction();
        CosFunction realCos = new CosFunction(realSin);
        CotFunction cot = new CotFunction(realSin, realCos);

        assertEquals(1.0,  cot.compute(Math.PI / 4, EPS), DELTA);
        assertEquals(0.0,  cot.compute(Math.PI / 2, EPS), DELTA);
    }
}
