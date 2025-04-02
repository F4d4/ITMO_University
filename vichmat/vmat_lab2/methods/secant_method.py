from scipy.differentiate import derivative
from dto.result import Result
from methods.method import Method, MAX_ITERATIONS


class SecantMethod(Method):
    """
    Класс для реализации метода секущих
    """
    name = "Метод секущих"

    def solve(self) -> Result:
        """
        Метод секущих:
        - новое приближение определяется двумя предыдущими итерациями x_i и x_(i - 1)
        - выбор x_0 определяется как в методе Ньютона
        - x_1 выбирается рядом с начальным самостоятельно, например x_1 = x_0 + eps
        :return: Результат решения — объект Result
        """
        f = self.equation.function
        a = self.a
        b = self.b
        eps = self.eps
        iterations = 0

        fa_ = derivative(f, a).df
        fb_ = derivative(f, b).df

        # Проверяем условие сходимости метода
        if fa_ * fb_ < 0:
            print('Условие сходимости метода секущих не выполнено')

        x0 = a
        if f(a) * fb_ > 0:
            x0 = a
        if f(b) * fb_ > 0:
            x0 = b

        x1 = x0 + eps

        while True:
            if iterations == MAX_ITERATIONS:
                raise Exception(f'Выполнено {MAX_ITERATIONS} итераций. Решение н найдено')
            iterations += 1

            x = x1 - f(x1) * (x1 - x0) / (f(x1) - f(x0))
            delta = abs(x - x1)

            # Проверяем достижение необходимой точности
            if delta < eps:
                break

            x0 = x1
            x1 = x

        return Result(x, iterations)