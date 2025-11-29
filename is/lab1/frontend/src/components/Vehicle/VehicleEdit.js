import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { vehicleService } from '../../services/api';
import VehicleForm from './VehicleForm';

const VehicleEdit = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [errors, setErrors] = useState({});

  const [formData, setFormData] = useState({
    name: '',
    x: 0,
    y: 0,
    type: '',
    enginePower: 1,
    numberOfWheels: 4,
    capacity: 1,
    distanceTravelled: 0,
    fuelConsumption: 1,
    fuelType: '',
  });

  useEffect(() => {
    loadVehicle();
  }, [id]);

  const loadVehicle = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await vehicleService.getById(id);
      const vehicle = response.data;

      setFormData({
        name: vehicle.name,
        x: vehicle.x,
        y: vehicle.y,
        type: vehicle.type || '',
        enginePower: vehicle.enginePower,
        numberOfWheels: vehicle.numberOfWheels,
        capacity: vehicle.capacity,
        distanceTravelled: vehicle.distanceTravelled,
        fuelConsumption: vehicle.fuelConsumption,
        fuelType: vehicle.fuelType || '',
      });
    } catch (err) {
      setError(err.message || 'Не удалось загрузить данные транспортного средства');
    } finally {
      setLoading(false);
    }
  };

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
      }
    }

    setFormData((prev) => ({
      ...prev,
      [name]: parsedValue,
    }));
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

    if (formData.y > 621) {
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

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    try {
      setSaving(true);
      setError(null);

      const dataToSend = {
        name: formData.name,
        x: Number(formData.x),
        y: Number(formData.y),
        type: formData.type || null,
        enginePower: Number(formData.enginePower),
        numberOfWheels: Number(formData.numberOfWheels),
        capacity: Number(formData.capacity),
        distanceTravelled: Number(formData.distanceTravelled),
        fuelConsumption: Number(formData.fuelConsumption),
        fuelType: formData.fuelType || null,
      };

      await vehicleService.update(id, dataToSend);
      navigate(`/vehicles/${id}`);
    } catch (err) {
      setError(err.message || 'Не удалось обновить транспортное средство');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="loading">
        <div className="loading-spinner"></div>
        <p>Загрузка...</p>
      </div>
    );
  }

  return (
    <div>
      <h1 className="page-title">✏️ Редактирование транспортного средства</h1>
      <p className="page-subtitle">ID: {id}</p>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="card">
        {saving ? (
          <div className="loading">
            <div className="loading-spinner"></div>
            <p>Сохранение...</p>
          </div>
        ) : (
          <VehicleForm
            formData={formData}
            errors={errors}
            onChange={handleChange}
            onSubmit={handleSubmit}
            isEdit={true}
          />
        )}
      </div>
    </div>
  );
};

export default VehicleEdit;






