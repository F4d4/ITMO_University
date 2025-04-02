from dto.equation import Equation
from dto.result import Result

MAX_ITERATIONS = 1000

class Method:
    """
    Абстрактный класс для численного метода решения уравнений
    """
    name = None

    def __init__(self, equation: Equation, a: float, b: float, eps: float, decimal_places: int):
        self.equation = equation
        self.a = a
        self.b = b
        self.eps = eps
        self.decimal_places = decimal_places

    def solve(self) -> Result:
        """
        Метод для запуска решения
        :return: Результат решения — объект Result
        """
        pass