import numpy as np
from scipy.differentiate import derivative
from dto.result import Result
from methods.method import Method, MAX_ITERATIONS


class SimpleIterationsMethod(Method):
    """
    Класс для реализации метода простой итерации
    """
    name = 'Метод простой итерации'

    def solve(self) -> Result:
        """
        Метод простой итераций:
        1. преобразуем уравнение f(x) = 0 к равносильному (при _lambda != 0) _lambda * f(x) = 0
        2. прибавляем x в обеих частях: x = x + _lambda * f(x)
        3. phi(x) = x + _lambda * f(x), phi'(x) = 1 + _lambda * f'(x)
        4. высокая сходимость  обеспечивается при q = max(|phi'(x)|) примерно равное 0. Тогда _lambda = 1/max(|f'(x)|) (с минусом, если f'[a, b] > 0)
        :return: Результат решения — объект Result
        """
        f = self.equation.function
        a = self.a
        b = self.b
        eps = self.eps
        iterations = 0

        fa_ = derivative(f, a).df
        fb_ = derivative(f, b).df

        max_derivative = max(abs(fa_), abs(fb_))
        _lambda = 1 / max_derivative
        if fa_ > 0: _lambda *= -1

        phi = lambda x: x + _lambda * f(x)
        phi_ = lambda x: derivative(phi, x).df

        q = np.max(abs(phi_(np.linspace(a, b, int(1 / eps)))))
        if q > 1:
            print(f'Метод не сходится, так как q >= 1')

        prev_x = a

        while True:
            if iterations == MAX_ITERATIONS:
                raise Exception(f'Выполнено {MAX_ITERATIONS} итераций. Решение н найдено')
            iterations += 1

            x = phi(prev_x)
            delta = abs(x - prev_x)

            if delta < eps:
                break

            prev_x = x

        return Result(x, iterations)