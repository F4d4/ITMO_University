package functions.log;

import functions.base.LnFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Тесты для Log2Function, Log3Function, Log10Function.
 *
 * В тестах на основе заглушек LnFunction заменяется табличной заглушкой,
 * которая возвращает предвычисленные значения, изолируя каждую лог-функцию от базовой реализации.
 */
@ExtendWith(MockitoExtension.class)
class LogFunctionTest {

    private static final double EPS   = 1e-9;
    private static final double DELTA = 1e-6;

    @Mock private LnFunction lnStub;

    // Вспомогательный метод: подстановка табличных значений ln для log_b(x) = ln(x)/ln(b)
    private void stubLn(double x, double value) {
        when(lnStub.compute(eq(x), anyDouble())).thenReturn(value);
    }

    // ---- Тесты Log2 ----

    @Test
    void log2OfOneIsZero_withStub() {
        stubLn(1.0, 0.0);
        stubLn(2.0, Math.log(2));
        assertEquals(0.0, new Log2Function(lnStub).compute(1.0, EPS), DELTA);
    }

    @Test
    void log2OfTwoIsOne_withStub() {
        stubLn(2.0, Math.log(2));
        assertEquals(1.0, new Log2Function(lnStub).compute(2.0, EPS), DELTA);
    }

    @Test
    void log2OfFourIsTwo_withStub() {
        stubLn(4.0, Math.log(4));
        stubLn(2.0, Math.log(2));
        assertEquals(2.0, new Log2Function(lnStub).compute(4.0, EPS), DELTA);
    }

    @Test
    void log2ThrowsForNonPositive_withStub() {
        assertThrows(ArithmeticException.class, () -> new Log2Function(lnStub).compute(0.0, EPS));
        assertThrows(ArithmeticException.class, () -> new Log2Function(lnStub).compute(-3.0, EPS));
    }

    // ---- Тесты Log3 ----

    @Test
    void log3OfOneIsZero_withStub() {
        stubLn(1.0, 0.0);
        stubLn(3.0, Math.log(3));
        assertEquals(0.0, new Log3Function(lnStub).compute(1.0, EPS), DELTA);
    }

    @Test
    void log3OfThreeIsOne_withStub() {
        stubLn(3.0, Math.log(3));
        assertEquals(1.0, new Log3Function(lnStub).compute(3.0, EPS), DELTA);
    }

    @Test
    void log3OfNineIsTwo_withStub() {
        stubLn(9.0, Math.log(9));
        stubLn(3.0, Math.log(3));
        assertEquals(2.0, new Log3Function(lnStub).compute(9.0, EPS), DELTA);
    }

    // ---- Тесты Log10 ----

    @Test
    void log10OfOneIsZero_withStub() {
        stubLn(1.0, 0.0);
        stubLn(10.0, Math.log(10));
        assertEquals(0.0, new Log10Function(lnStub).compute(1.0, EPS), DELTA);
    }

    @Test
    void log10OfTenIsOne_withStub() {
        stubLn(10.0, Math.log(10));
        assertEquals(1.0, new Log10Function(lnStub).compute(10.0, EPS), DELTA);
    }

    @Test
    void log10OfHundredIsTwo_withStub() {
        stubLn(100.0, Math.log(100));
        stubLn(10.0, Math.log(10));
        assertEquals(2.0, new Log10Function(lnStub).compute(100.0, EPS), DELTA);
    }

    // ---- Интеграционные тесты: реальная LnFunction ----

    @Test
    void allLogFunctionsWithRealLn() {
        LnFunction realLn = new LnFunction();
        Log2Function  log2  = new Log2Function(realLn);
        Log3Function  log3  = new Log3Function(realLn);
        Log10Function log10 = new Log10Function(realLn);

        assertEquals(1.0, log2.compute(2.0,   EPS), DELTA);
        assertEquals(2.0, log2.compute(4.0,   EPS), DELTA);
        assertEquals(1.0, log3.compute(3.0,   EPS), DELTA);
        assertEquals(2.0, log3.compute(9.0,   EPS), DELTA);
        assertEquals(1.0, log10.compute(10.0, EPS), DELTA);
        assertEquals(2.0, log10.compute(100.0,EPS), DELTA);
    }

    @Test
    void changeOfBaseIdentity_withRealLn() {
        // log_2(x) * log_3(2) = log_3(x)   (проверка формулы смены основания)
        LnFunction realLn = new LnFunction();
        Log2Function  log2 = new Log2Function(realLn);
        Log3Function  log3 = new Log3Function(realLn);

        double x = 5.0;
        double expected = log3.compute(x, EPS);
        double actual   = log2.compute(x, EPS) * log3.compute(2.0, EPS);
        assertEquals(expected, actual, DELTA);
    }
}
