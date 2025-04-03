from dto.result import Result
from methods.method import Method, MAX_ITERATIONS


class ChordMethod(Method):
    """
    Класс для реализации метода хорд
    """
    name = 'Метод хорд'

    def solve(self) -> Result:
        """
        Метод хорд:
        1. находим интервал изоляции корня [a0, b0]
        2. вычисляем x0
        3. вычисляем f(x0)
        4. в качестве нового интервала выбираем ту половину отрезка, на концах которой функция имеет разные знаки
        5. вычисляем x1 и т. д. (повторяем шаги 2-4)
        :return: Результат решения — объект Result
        """
        f = self.equation.function
        a, b = self.a, self.b
        eps = self.eps
        iterations = 0

        # Формула хорд
        x = (a * f(b) - b * f(a)) / (f(b) - f(a))

        while True:
            if iterations == MAX_ITERATIONS:
                raise Exception(f'Выполнено {MAX_ITERATIONS} итераций. Решение не найдено')
            iterations += 1

            # Обновляем границы
            if f(a) * f(x) <= 0:
                b = x
            else:
                a = x

            new_x = (a * f(b) - b * f(a)) / (f(b) - f(a))
            delta = abs(new_x - x)

            # Проверяем достижение необходимой точности
            if delta < eps and abs(f(new_x)) < eps:
                x = new_x
                break

            x = new_x

        return Result(x, iterations)