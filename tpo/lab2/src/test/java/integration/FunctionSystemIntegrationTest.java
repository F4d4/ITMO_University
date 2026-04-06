package integration;

import org.itmo.FunctionSystem;
import functions.base.LnFunction;
import functions.base.SinFunction;
import functions.log.Log10Function;
import functions.log.Log2Function;
import functions.log.Log3Function;
import functions.trig.CosFunction;
import functions.trig.CotFunction;
import functions.trig.TanFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Integration tests for FunctionSystem.
 *
 * Integration strategy (bottom-up, per the module dependency diagram):
 *   Level 1 – All stubs: verifies system wiring and branching.
 *   Level 2 – Real sin → cos → tan/cot integrated; stubs for logs (x ≤ 0 branch).
 *   Level 3 – Real ln → log2/3/10 integrated; stubs for trig (x > 0 branch).
 *   Level 4 – Full integration: all real functions, both branches.
 */
@ExtendWith(MockitoExtension.class)
class FunctionSystemIntegrationTest {

    private static final double EPS   = 1e-9;
    private static final double DELTA = 1e-5;

    @Mock private CotFunction  cotStub;
    @Mock private TanFunction  tanStub;
    @Mock private Log2Function log2Stub;
    @Mock private Log3Function log3Stub;
    @Mock private Log10Function log10Stub;

    // -------------------------------------------------------------------------
    // Level 1: All stubs – system wiring
    // -------------------------------------------------------------------------

    @Test
    void level1_negativeBranch_returnsProductOfCotAndTan() {
        double x = -Math.PI / 4;
        when(cotStub.compute(eq(x), anyDouble())).thenReturn(2.0);
        when(tanStub.compute(eq(x), anyDouble())).thenReturn(3.0);

        FunctionSystem system = new FunctionSystem(cotStub, tanStub, log2Stub, log3Stub, log10Stub);
        assertEquals(6.0, system.compute(x, EPS), DELTA);
    }

    @Test
    void level1_positiveBranch_usesLogStubs() {
        double x = 2.0;
        // Stub log values so the formula is computable
        when(log2Stub.compute(eq(x), anyDouble())).thenReturn(1.0);
        when(log10Stub.compute(eq(x), anyDouble())).thenReturn(0.30103);
        when(log3Stub.compute(eq(x), anyDouble())).thenReturn(0.63093);

        // ((((1/0.30103)^2) * 0.63093^2)^2) * 1
        double a = 1.0 / 0.30103;
        double expected = Math.pow(Math.pow(a * a * 0.63093 * 0.63093, 2), 1) * 1.0;
        // Recompute exactly as in FunctionSystem
        double b = a * a;
        double c = 0.63093 * 0.63093;
        double d = b * c;
        double e = d * d;
        expected = e * 1.0;

        FunctionSystem system = new FunctionSystem(cotStub, tanStub, log2Stub, log3Stub, log10Stub);
        assertEquals(expected, system.compute(x, EPS), DELTA);
    }

    @Test
    void level1_zeroIsNegativeBranch_cotUndefinedThrows() {
        when(cotStub.compute(eq(0.0), anyDouble()))
                .thenThrow(new ArithmeticException("cot undefined at 0"));

        FunctionSystem system = new FunctionSystem(cotStub, tanStub, log2Stub, log3Stub, log10Stub);
        assertThrows(ArithmeticException.class, () -> system.compute(0.0, EPS));
    }

    // -------------------------------------------------------------------------
    // Level 2: Real trig chain (sin → cos → tan, cot); stubs for logs (x ≤ 0)
    // -------------------------------------------------------------------------

    @Test
    void level2_negativeBranch_cotTimesTanEqualsOne() {
        SinFunction sin = new SinFunction();
        CosFunction cos = new CosFunction(sin);
        CotFunction cot = new CotFunction(sin, cos);
        TanFunction tan = new TanFunction(sin, cos);

        FunctionSystem system = new FunctionSystem(cot, tan, log2Stub, log3Stub, log10Stub);

        // cot(x)*tan(x) = cos(x)/sin(x) * sin(x)/cos(x) = 1 for valid x
        assertEquals(1.0, system.compute(-Math.PI / 4, EPS), DELTA);
        assertEquals(1.0, system.compute(-1.0,         EPS), DELTA);
        assertEquals(1.0, system.compute(-0.5,         EPS), DELTA);
    }

    @Test
    void level2_negativeBranch_atPiSinIsZero_cotThrows() {
        SinFunction sin = new SinFunction();
        CosFunction cos = new CosFunction(sin);
        CotFunction cot = new CotFunction(sin, cos);
        TanFunction tan = new TanFunction(sin, cos);

        FunctionSystem system = new FunctionSystem(cot, tan, log2Stub, log3Stub, log10Stub);
        // sin(−π) ≈ 0 → cot undefined
        assertThrows(ArithmeticException.class, () -> system.compute(-Math.PI, EPS));
    }

    // -------------------------------------------------------------------------
    // Level 3: Real log chain (ln → log2/3/10); stubs for trig (x > 0)
    // -------------------------------------------------------------------------

    @Test
    void level3_positiveBranch_knownValueAtX2() {
        LnFunction    ln    = new LnFunction();
        Log2Function  log2  = new Log2Function(ln);
        Log3Function  log3  = new Log3Function(ln);
        Log10Function log10 = new Log10Function(ln);

        FunctionSystem system = new FunctionSystem(cotStub, tanStub, log2, log3, log10);

        // Expected using standard Math.log for reference
        double x = 2.0;
        double l2  = Math.log(x) / Math.log(2);
        double l10 = Math.log(x) / Math.log(10);
        double l3  = Math.log(x) / Math.log(3);
        double expected = Math.pow(Math.pow(l2 / l10, 2) * Math.pow(l3, 2), 2) * l2;

        assertEquals(expected, system.compute(x, EPS), DELTA);
    }

    @Test
    void level3_positiveBranch_xEqualsOneIsUndefined() {
        // At x=1: log2(1)=0, log10(1)=0 → division by zero (or 0/0)
        LnFunction    ln    = new LnFunction();
        Log2Function  log2  = new Log2Function(ln);
        Log3Function  log3  = new Log3Function(ln);
        Log10Function log10 = new Log10Function(ln);

        FunctionSystem system = new FunctionSystem(cotStub, tanStub, log2, log3, log10);
        // log2(1)/log10(1) = 0/0 → NaN, but no exception from logs themselves; result is NaN
        double result = system.compute(1.0, EPS);
        assertTrue(Double.isNaN(result) || Double.isInfinite(result));
    }

    // -------------------------------------------------------------------------
    // Level 4: Full integration – all real functions
    // -------------------------------------------------------------------------

    @Test
    void level4_fullIntegration_negativeBranch_isAlwaysOne() {
        FunctionSystem system = buildFullSystem();

        double[] xs = {-0.1, -0.5, -1.0, -Math.PI / 4, -Math.PI / 3};
        for (double x : xs) {
            assertEquals(1.0, system.compute(x, EPS), DELTA,
                    "Expected cot(x)*tan(x)=1 at x=" + x);
        }
    }

    @Test
    void level4_fullIntegration_positiveBranch_x2() {
        FunctionSystem system = buildFullSystem();

        double x = 2.0;
        double l2  = Math.log(x) / Math.log(2);
        double l10 = Math.log(x) / Math.log(10);
        double l3  = Math.log(x) / Math.log(3);
        double expected = Math.pow(Math.pow(l2 / l10, 2) * Math.pow(l3, 2), 2) * l2;

        assertEquals(expected, system.compute(x, EPS), DELTA);
    }

    @Test
    void level4_fullIntegration_positiveBranch_x10() {
        FunctionSystem system = buildFullSystem();

        double x = 10.0;
        double l2  = Math.log(x) / Math.log(2);
        double l10 = Math.log(x) / Math.log(10);
        double l3  = Math.log(x) / Math.log(3);
        double expected = Math.pow(Math.pow(l2 / l10, 2) * Math.pow(l3, 2), 2) * l2;

        assertEquals(expected, system.compute(x, EPS), DELTA);
    }

    @Test
    void level4_fullIntegration_nonPositiveLogArgThrows() {
        FunctionSystem system = buildFullSystem();
        // x = 0 is handled by trig branch but cot(0) is undefined
        assertThrows(ArithmeticException.class, () -> system.compute(0.0, EPS));
    }

    // -------------------------------------------------------------------------

    private FunctionSystem buildFullSystem() {
        SinFunction   sin   = new SinFunction();
        LnFunction    ln    = new LnFunction();
        CosFunction   cos   = new CosFunction(sin);
        TanFunction   tan   = new TanFunction(sin, cos);
        CotFunction   cot   = new CotFunction(sin, cos);
        Log2Function  log2  = new Log2Function(ln);
        Log3Function  log3  = new Log3Function(ln);
        Log10Function log10 = new Log10Function(ln);
        return new FunctionSystem(cot, tan, log2, log3, log10);
    }
}
