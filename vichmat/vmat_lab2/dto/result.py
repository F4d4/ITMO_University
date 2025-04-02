from dataclasses import dataclass
from typing import Optional, List


@dataclass
class Result:
    """
    Класс-результат для хранения данных после численного решения уравнения или системы.
    """
    x: float
    iterations: int
    # y: Optional[float]

    def __str__(self):
        parts = [f"Корень: x = {self.x}"]
        if self.y is not None:
            parts.append(f"y = {self.y}")
        parts.append(f"Число итераций: {self.iterations}")
        return "\n".join(parts)