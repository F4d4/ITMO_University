from typing import Callable
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

        fa = f(a)
        fb = f(b)

        fa_ = derivative(f, a).df
        fb_ = derivative(f, b).df

        return (fa * fb < 0) and (fa_ * fb_ > 0)