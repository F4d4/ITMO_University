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
 * Интеграционные тесты для FunctionSystem.
 *
 * Стратегия интеграции (снизу вверх, по диаграмме зависимостей модулей):
 *    Все заглушки: проверка связности системы и ветвления.
 *    Интеграция реальной цепочки sin -> cos -> tan/cot; заглушки для логов (ветвь x <= 0).
 *    Интеграция реальной цепочки ln -> log2/3/10; заглушки для тригонометрии (ветвь x > 0).
 *    Полная интеграция: все функции реальные, обе ветви.
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
    // все заглушки – связность системы
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
        // Подставляем значения логарифмов-заглушек, чтобы формула была вычислима
        when(log2Stub.compute(eq(x), anyDouble())).thenReturn(1.0);
        when(log10Stub.compute(eq(x), anyDouble())).thenReturn(0.30103);
        when(log3Stub.compute(eq(x), anyDouble())).thenReturn(0.63093);

        // ((((1/0.30103)^2) * 0.63093^2)^2) * 1
        double a = 1.0 / 0.30103;
        double expected = Math.pow(Math.pow(a * a * 0.63093 * 0.63093, 2), 1) * 1.0;
        // Пересчитываем точно так же, как в FunctionSystem
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
    // реальная тригонометрическая цепочка (sin -> cos -> tan, cot); заглушки для логов (x <= 0)
    @Test
    void level2_negativeBranch_cotTimesTanEqualsOne() {
        SinFunction sin = new SinFunction();
        CosFunction cos = new CosFunction(sin);
        CotFunction cot = new CotFunction(sin, cos);
        TanFunction tan = new TanFunction(sin, cos);

        FunctionSystem system = new FunctionSystem(cot, tan, log2Stub, log3Stub, log10Stub);

        // cot(x)*tan(x) = cos(x)/sin(x) * sin(x)/cos(x) = 1 для допустимых x
        assertAll(
                () -> assertEquals(1.0, system.compute(-Math.PI / 4, EPS), DELTA),
                () -> assertEquals(1.0, system.compute(-1.0,         EPS), DELTA),
                () -> assertEquals(1.0, system.compute(-0.5,         EPS), DELTA)
        );
    }

    @Test
    void level2_negativeBranch_atPiSinIsZero_cotThrows() {
        SinFunction sin = new SinFunction();
        CosFunction cos = new CosFunction(sin);
        CotFunction cot = new CotFunction(sin, cos);
        TanFunction tan = new TanFunction(sin, cos);

        FunctionSystem system = new FunctionSystem(cot, tan, log2Stub, log3Stub, log10Stub);
        // sin(−pi) ≈ 0 -> cot не определен
        assertThrows(ArithmeticException.class, () -> system.compute(-Math.PI, EPS));
    }
    // реальная логарифмическая цепочка (ln -> log2/3/10); заглушки для тригонометрии (x > 0)
    @Test
    void level3_positiveBranch_knownValueAtX2() {
        LnFunction    ln    = new LnFunction();
        Log2Function  log2  = new Log2Function(ln);
        Log3Function  log3  = new Log3Function(ln);
        Log10Function log10 = new Log10Function(ln);

        FunctionSystem system = new FunctionSystem(cotStub, tanStub, log2, log3, log10);

        // Ожидаемое значение считаем через стандартный Math.log как эталон
        double x = 2.0;
        double l2  = Math.log(x) / Math.log(2);
        double l10 = Math.log(x) / Math.log(10);
        double l3  = Math.log(x) / Math.log(3);
        double expected = Math.pow(Math.pow(l2 / l10, 2) * Math.pow(l3, 2), 2) * l2;

        assertEquals(expected, system.compute(x, EPS), DELTA);
    }

    @Test
    void level3_positiveBranch_xEqualsOneIsUndefined() {
        // При x=1: log2(1)=0, log10(1)=0 -> деление на ноль (или 0/0)
        LnFunction    ln    = new LnFunction();
        Log2Function  log2  = new Log2Function(ln);
        Log3Function  log3  = new Log3Function(ln);
        Log10Function log10 = new Log10Function(ln);

        FunctionSystem system = new FunctionSystem(cotStub, tanStub, log2, log3, log10);
        // log2(1)/log10(1) = 0/0 -> NaN, но сами логарифмы не бросают исключение; результат NaN
        double result = system.compute(1.0, EPS);
        assertTrue(Double.isNaN(result) || Double.isInfinite(result));
    }
    // полная интеграция – все функции реальные
    @Test
    void level4_fullIntegration_negativeBranch_isAlwaysOne() {
        FunctionSystem system = buildFullSystem();

        double[] xs = {-0.1, -0.5, -1.0, -Math.PI / 4, -Math.PI / 3};
        for (double x : xs) {
            assertEquals(1.0, system.compute(x, EPS), DELTA,
                    "Ожидалось cot(x)*tan(x)=1 при x=" + x);
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
    void level4_fullIntegration_nonPositiveLogArgThrows() {
        FunctionSystem system = buildFullSystem();
        // x = 0 попадает в тригонометрическую ветвь, но cot(0) не определен
        assertThrows(ArithmeticException.class, () -> system.compute(0.0, EPS));
    }

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
