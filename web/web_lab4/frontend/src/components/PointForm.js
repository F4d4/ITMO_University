import React, { useState } from 'react';
import './css/PointForm.css';

const Xvalues = [-2, -1.5, -1, -0.5, 0, 0.5, 1, 1.5, 2];

const Rvalues = [0.5, 1, 1.5, 2];

function PointForm({ onSubmit, onRChange }) {
    const [x, setX] = useState(null);
    const [y, setY] = useState('');
    const [r, setR] = useState(null);
    const [errors, setErrors] = useState({});

    const validate = () => {
        const newErrors = {};

        if (x === null) {
            newErrors.x = 'Выберите значение X';
        }

        const yNumber = parseFloat(y);
        if (isNaN(yNumber) || yNumber < -5 || yNumber > 3) {
            newErrors.y = 'Y должен быть числом от -5 до 3';
        }

        if (r === null) {
            newErrors.r = 'Выберите значение R';
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        if (validate()) {
            onSubmit({ x: parseFloat(x), y: parseFloat(y), r: parseFloat(r) });
        }
    };

    // Логика для чекбоксов X: сбрасываем, если нажали уже выбранный;
    // в противном случае переключаемся на новое значение.
    const handleXCheckbox = (value) => {
        setX((prev) => (prev === value ? null : value));
    };

    // Аналогичная логика для чекбоксов R.
    const handleRCheckbox = (value) => {
        setR((prev) => (prev === value ? null : value));
        onRChange(value);
    };

    return (
        <form onSubmit={handleSubmit} className="point-form">
            {/* Блок для выбора X */}
            <div className="form-group">
                <label>Координата X:</label>
                <div className="checkbox-group">
                    {Xvalues.map((value) => (
                        <label key={value} className="checkbox-label">
                            <input
                                type="checkbox"
                                checked={x === value}
                                onChange={() => handleXCheckbox(value)}
                            />
                            {value}
                        </label>
                    ))}
                </div>
                {errors.x && <span className="error">{errors.x}</span>}
            </div>

            {/* Блок для ввода Y */}
            <div className="form-group">
                <label>Координата Y (-5 ... 3):</label>
                <input
                    type="text"
                    value={y}
                    onChange={(e) => {
                        const value = e.target.value;
                        if (value === '' || /^-?\d*(\.\d{0,2})?$/.test(value)) {
                            setY(value);
                        }
                    }}
                    placeholder="Введите Y"
                />
                {errors.y && <span className="error">{errors.y}</span>}
            </div>



            {/* Блок для выбора R */}
            <div className="form-group">
                <label>Радиус R:</label>
                <div className="checkbox-group">
                    {Rvalues.map((value) => (
                        <label key={value} className="checkbox-label">
                            <input
                                type="checkbox"
                                checked={r === value}
                                onChange={() => handleRCheckbox(value)}
                            />
                            {value}
                        </label>
                    ))}
                </div>
                {errors.r && <span className="error">{errors.r}</span>}
            </div>

            <button type="submit" className="submit-btn">
                Проверить
            </button>
        </form>
    );
}

export default PointForm;
