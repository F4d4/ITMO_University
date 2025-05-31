import numpy as np
import matplotlib.pyplot as plt
import argparse
import sys
import contextlib

def read_data(file_path=None):
    if file_path:
        data = np.loadtxt(file_path)
        x, y = data[:, 0], data[:, 1]
    else:
        print("Введите значения x и y, по одной паре на строку (8-12 точек). Пустая строка для завершения ввода:")
        xs, ys = [], []
        for line in sys.stdin:
            line = line.strip()
            if not line:
                break
            parts = line.split()
            if len(parts) != 2:
                print("Неверная строка, ожидается два числа.")
                continue
            xi, yi = map(float, parts)
            xs.append(xi); ys.append(yi)
        x, y = np.array(xs), np.array(ys)
    if not (8 <= len(x) <= 12):
        print(f"Предупреждение: ожидалось 8-12 точек, получено {len(x)}")
    return x, y

# Базисные функции
def basis_lin(x): return np.vstack([np.ones_like(x), x]).T
def basis_poly(x, deg): return np.vstack([x**i for i in range(deg+1)]).T

def fit_least_squares(P, y):
    a, *_ = np.linalg.lstsq(P, y, rcond=None)
    return a

# Вычисление модели
def eval_model(x, coeffs, name):
    if name == 'linear':
        return coeffs[0] + coeffs[1]*x
    if name.startswith('poly'):
        deg = int(name[-1])
        return sum(coeffs[i]*x**i for i in range(deg+1))
    if name == 'exponential':
        A, B = coeffs
        return A * np.exp(B*x)
    if name == 'logarithmic':
        A, B = coeffs
        return A + B * np.log(x)
    if name == 'power':
        A, B = coeffs
        return A * x**B
    raise ValueError(name)

# Аппроксимация каждой модели
def fit_models(x, y):
    results = {}
    # Линейная
    P = basis_lin(x)
    results['linear'] = fit_least_squares(P, y)
    # Полиномы 2-ой и 3-ей степени
    for deg in [2, 3]:
        P = basis_poly(x, deg)
        results[f'poly{deg}'] = fit_least_squares(P, y)
    # Экспоненциальная: ln y = ln A + B x
    if np.all(y > 0):
        yp = np.log(y)
        P = basis_lin(x)
        ab = fit_least_squares(P, yp)
        A, B = np.exp(ab[0]), ab[1]
        results['exponential'] = np.array([A, B])
    # Логарифмическая: y = A + B ln x
    if np.all(x > 0):
        P = np.vstack([np.ones_like(x), np.log(x)]).T
        results['logarithmic'] = fit_least_squares(P, y)
    # Степенная: ln y = ln A + B ln x
    if np.all(x > 0) and np.all(y > 0):
        xp, yp = np.log(x), np.log(y)
        P = basis_lin(xp)
        ab = fit_least_squares(P, yp)
        A, B = np.exp(ab[0]), ab[1]
        results['power'] = np.array([A, B])
    return results

# Метрики качества
def compute_metrics(x, y, coeffs, name):
    y_pred = eval_model(x, coeffs, name)
    eps = y - y_pred
    rms = np.sqrt(np.mean(eps**2))
    R2 = 1 - np.sum(eps**2)/np.sum((y - np.mean(y))**2)
    return y_pred, eps, rms, R2

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-f', '--file', help='входной файл с двумя колонками: x y')
    parser.add_argument('-o', '--out', help='файл, куда записать результаты')
    args = parser.parse_args()

    x, y = read_data(args.file)
    models = fit_models(x, y)

    # 3. выбираем, куда печатать: либо stdout, либо указанный файл
    stream = open(args.out, 'w', encoding='utf-8') if args.out else sys.stdout
    with contextlib.redirect_stdout(stream):
        stats = {}
        for name, coeffs in models.items():
            try:
                y_pred, eps, rms, R2 = compute_metrics(x, y, coeffs, name)
            except Exception:
                continue
            stats[name] = (coeffs, y_pred, eps, rms, R2)

            # Вывод результатов
            print(f"Модель: {name}")
            print(" Коэффициенты:", coeffs)
            print(f" Среднеквадратичное отклонение: {rms:.6f}")
            print(f" Коэффициент детерминации (R^2): {R2:.6f}")
            if name == 'linear':
                corr = np.corrcoef(x, y)[0,1]
                print(f" Коэффициент корреляции Пирсона: {corr:.6f}")
                if R2 >= 0.8:
                    print(" Высокая степень детерминации.")
                elif R2 >= 0.5:
                    print(" Средняя степень детерминации.")
                else:
                    print(" Низкая степень детерминации.")
            print(" x, y, φ(x), ε:")
            for xi, yi, ph, e in zip(x, y, y_pred, eps):
                print(f" {xi:.3f}, {yi:.3f}, {ph:.3f}, {e:.3f}")
            print()

        # Выбор всех лучших моделей по минимальному RMS
        rms_values = [vals[3] for vals in stats.values()]
        min_rms = min(rms_values)
        best_models = [
            name
            for name, (_c, _yp, _e, rms, _r2) in stats.items()
            if abs(rms - min_rms) < 1e-12
        ]
        if len(best_models) == 1:
            print(f"Лучшая модель: {best_models[0]}")
        else:
            print(f"Лучшие модели: {', '.join(best_models)} ")

    if args.out:
        stream.close()
        print(f'Результаты сохранены в {args.out}')

    # График (без изменений)
    lo, hi = np.min(x), np.max(x)
    dx = (hi - lo) * 0.1
    xs = np.linspace(lo - dx, hi + dx, 300)
    plt.scatter(x, y, label='Исходные данные')
    for name, (coeffs, *_ ) in stats.items():
        try:
            ys = eval_model(xs, coeffs, name)
            plt.plot(xs, ys, label=name)
        except Exception:
            continue
    plt.legend()
    plt.title('Аппроксимация методом наименьших квадратов')
    plt.show()
