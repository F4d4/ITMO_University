import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { vehicleService } from '../../services/api';
import VehicleForm from './VehicleForm';

const VehicleCreate = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [errors, setErrors] = useState({});

  const [formData, setFormData] = useState({
    name: '',
    coordinatesId: '',
    x: '',
    y: '',
    type: '',
    enginePower: 1,
    numberOfWheels: 4,
    capacity: 1,
    distanceTravelled: 0,
    fuelConsumption: 1,
    fuelType: '',
  });

  const handleChange = (e) => {
    const { name, value, type } = e.target;
    let parsedValue = value;

    // Правильно парсим числовые значения
    if (type === 'number') {
      if (name === 'capacity' || name === 'x') {
        parsedValue = value === '' ? '' : parseFloat(value);
      } else if (name === 'y' || name === 'enginePower' || name === 'numberOfWheels' || 
                 name === 'distanceTravelled' || name === 'fuelConsumption') {
        parsedValue = value === '' ? '' : parseInt(value, 10);
      } else if (name === 'coordinatesId') {
        parsedValue = value === '' ? '' : parseInt(value, 10);
      }
    }

    setFormData((prev) => ({
      ...prev,
      [name]: parsedValue,
    }));
    // Очищаем ошибку для этого поля
    if (errors[name]) {
      setErrors((prev) => ({
        ...prev,
        [name]: null,
      }));
    }
  };

  const validateForm = () => {
    const newErrors = {};

    if (!formData.name || formData.name.trim() === '') {
      newErrors.name = 'Название не может быть пустым';
    }

    // Проверяем координаты
    if (!formData.coordinatesId && (!formData.x || !formData.y)) {
      if (!formData.x) newErrors.x = 'Необходимо указать X координату';
      if (!formData.y) newErrors.y = 'Необходимо указать Y координату';
    }

    if (formData.y && formData.y > 621) {
      newErrors.y = 'Y координата не может превышать 621';
    }

    if (formData.enginePower <= 0) {
      newErrors.enginePower = 'Мощность двигателя должна быть больше 0';
    }

    if (formData.numberOfWheels <= 0) {
      newErrors.numberOfWheels = 'Количество колес должно быть больше 0';
    }

    if (formData.capacity <= 0) {
      newErrors.capacity = 'Вместимость должна быть больше 0';
    }

    if (formData.distanceTravelled < 0) {
      newErrors.distanceTravelled = 'Пройденное расстояние не может быть отрицательным';
    }

    if (formData.fuelConsumption <= 0) {
      newErrors.fuelConsumption = 'Расход топлива должен быть больше 0';
    }

    if (!formData.type || formData.type === '') {
      newErrors.type = 'Необходимо выбрать тип транспортного средства';
    }

    if (!formData.fuelType || formData.fuelType === '') {
      newErrors.fuelType = 'Необходимо выбрать тип топлива';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    try {
      setLoading(true);
      setError(null);

      // Подготовка данных для отправки
      const dataToSend = {
        name: formData.name,
        type: formData.type || null,
        enginePower: Number(formData.enginePower),
        numberOfWheels: Number(formData.numberOfWheels),
        capacity: Number(formData.capacity),
        distanceTravelled: Number(formData.distanceTravelled),
        fuelConsumption: Number(formData.fuelConsumption),
        fuelType: formData.fuelType || null,
      };

      // Добавляем либо coordinatesId, либо x/y
      if (formData.coordinatesId) {
        dataToSend.coordinatesId = Number(formData.coordinatesId);
      } else {
        dataToSend.x = Number(formData.x);
        dataToSend.y = Number(formData.y);
      }

      await vehicleService.create(dataToSend);
      navigate('/vehicles');
    } catch (err) {
      setError(err.message || 'Не удалось создать транспортное средство');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h1 className="page-title">➕ Создать новое транспортное средство</h1>
      <p className="page-subtitle">Заполните все обязательные поля формы</p>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="card">
        {loading ? (
          <div className="loading">
            <div className="loading-spinner"></div>
            <p>Создание...</p>
          </div>
        ) : (
          <VehicleForm
            formData={formData}
            errors={errors}
            onChange={handleChange}
            onSubmit={handleSubmit}
            isEdit={false}
          />
        )}
      </div>
    </div>
  );
};

export default VehicleCreate;






