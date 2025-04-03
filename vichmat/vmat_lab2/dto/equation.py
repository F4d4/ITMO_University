from typing import Callable

import numpy as np
from scipy.differentiate import derivative


class Equation:
    """
    Класс представляющий мат. функцию и её описание
    """

    def __init__(self, function: Callable, description: str):
        self.function = function
        self.description = description


    def root_exists(self, a: float, b: float):
        """
        Проверяет наличие корня функции на отрезке [a, b] по признаку смены знака.
        :param a: Левая граница отрезка.
        :param b: Правая граница отрезка.
        :return: True, если функция имеет решения на выбранном отрезке.
        """
        f = self.function
        n_points = 1000
        xs = np.linspace(a, b, n_points)
        tolerance = 1e-7

        roots = []
        i = 0
        while i < n_points - 1:
            x1 = xs[i]
            x2 = xs[i + 1]
            f1 = f(x1)
            f2 = f(x2)


            if abs(f1) < tolerance:
                if not roots or abs(x1 - roots[-1]) > (b - a) / n_points:
                    roots.append(x1)
                i += 1
                continue


            if f1 * f2 < 0:
                r = (x1 + x2) / 2
                if not roots or abs(r - roots[-1]) > (b - a) / n_points:
                    roots.append(r)
            i += 1

        if len(roots) == 0:
            print("На выбранном отрезке нет корней.")
            return False
        elif len(roots) > 1:
            print("На данном отрезке больше 1 корня, выберите отрезок, на котором будет только 1 корень функции.")
            return False
        else:
            return True