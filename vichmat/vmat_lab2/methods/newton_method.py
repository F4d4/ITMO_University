
import numpy as np
from dto.equation import Equation
from dto.result2 import Result2
from methods.method import MAX_ITERATIONS


def find_initial_approximation(system, x_range=(-2, 2), y_range=(-2, 2), step=0.1, threshold=0.5):
    """
    Автоматически ищет приближение (x, y)
    :param system: система уравнений
    :param x_range: диапазон по x
    :param y_range: диапазон по y
    :param step: шаг сетки
    :param threshold: максимальное значение |f(x, y)| и |g(x, y)| для выбора точки
    :return: (x0, y0) — подходящее приближение
    """
    f = system[0].function
    g = system[1].function

    for x in np.arange(x_range[0], x_range[1], step):
        for y in np.arange(y_range[0], y_range[1], step):
            if abs(f(x, y)) < threshold and abs(g(x, y)) < threshold:
                return x, y

    raise Exception("Не удалось найти начальное приближение. Попробуйте увеличить диапазон или threshold.")

def create_jacobian(x, y, system, h=1e-6):
    """
    Строит якобиан для системы из двух уравнений
    :param x, y: начальные приближения
    :param system: система уравнений
    :param h: шаг для численного дифференцирования (по умолчанию 1e-6)
    :return: якобиан 2x2
    """
    f = system[0].function
    g = system[1].function

    # частные производные по x, y для функции f(x, y)
    fx_ = (f(x + h, y) - f(x, y)) / h
    fy_ = (f(x, y + h) - f(x, y)) / h

    # частные производные по x, y для функции g(x, y)
    gx_ = (g(x + h, y) - g(x, y)) / h
    gy_ = (g(x, y + h) - g(x, y)) / h

    jac = [
        [fx_, fy_],
        [gx_, gy_],
    ]

    return jac

class NewtonMethod:
    """
    Класс для реализации метода Ньютона
    """
    name = "Метод Ньютона"

    def __init__(self, system: list[Equation], eps: float , x0:float , y0:float):
        self.system = system
        #self.x0, self.y0 = find_initial_approximation(system)
        self.x0 = x0;
        self.y0 = y0;
        self.eps = eps


    def solve(self) -> Result2:
        """
        Метод Ньютона:
        функция f(x) на отрезке [a, b] заменяется касательной и в качестве приближённого значения корня
        принимается точка пересечения касательной с осью абсцисс.
        :return:
        """
        system = self.system
        v = [self.x0, self.y0]  # Текущая точка (вектор)
        eps = self.eps
        iteration = 0


        while True:
            if iteration >= MAX_ITERATIONS:
                raise Exception(f'Выполнено {MAX_ITERATIONS} итераций. Решение не найдено.')
            iteration += 1

            jac = create_jacobian(v[0], v[1], system)

            f = system[0].function(v[0], v[1])
            g = system[1].function(v[0], v[1])

            F = [f, g]

            try:
                delta = np.linalg.solve(np.array(jac), -1 * np.array(F))
            except np.linalg.LinAlgError:
                raise Exception(f'Ошибка применения метода: Якобиан вырожден. Итерация->{iteration} Якобиан->{jac}')

            if np.max(np.abs(delta)) < eps:
                break

            next_v = v + delta

            v = next_v.tolist()

        return Result2(v[0], iteration, v[1])
