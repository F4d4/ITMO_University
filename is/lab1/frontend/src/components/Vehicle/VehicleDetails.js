import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { vehicleService } from '../../services/api';
import './VehicleDetails.css';

const VehicleDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [vehicle, setVehicle] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadVehicle();
  }, [id]);

  const loadVehicle = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await vehicleService.getById(id);
      setVehicle(response.data);
    } catch (err) {
      setError(err.message || 'Не удалось загрузить данные транспортного средства');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm('Вы уверены, что хотите удалить это транспортное средство?')) {
      return;
    }

    try {
      await vehicleService.delete(id);
      navigate('/vehicles');
    } catch (err) {
      setError(err.message || 'Не удалось удалить транспортное средство');
    }
  };

  const formatDate = (date) => {
    return new Date(date).toLocaleString('ru-RU');
  };

  if (loading) {
    return (
      <div className="loading">
        <div className="loading-spinner"></div>
        <p>Загрузка...</p>
      </div>
    );
  }

  if (error || !vehicle) {
    return (
      <div>
        <div className="alert alert-error">{error || 'Транспортное средство не найдено'}</div>
        <Link to="/vehicles" className="btn btn-secondary">
          ← Вернуться к списку
        </Link>
      </div>
    );
  }

  return (
    <div className="vehicle-details">
      <div className="details-header flex-between">
        <div>
          <h1 className="page-title">🚗 {vehicle.name}</h1>
          <p className="page-subtitle">ID: {vehicle.id}</p>
        </div>
        <div className="details-actions">
          <Link to={`/vehicles/${id}/edit`} className="btn btn-warning">
            ✏️ Редактировать
          </Link>
          <button onClick={handleDelete} className="btn btn-danger">
            🗑️ Удалить
          </button>
        </div>
      </div>

      <div className="details-grid">
        {/* Основная информация */}
        <div className="card">
          <div className="card-header">📝 Основная информация</div>
          <div className="details-content">
            <div className="detail-item">
              <span className="detail-label">Название:</span>
              <span className="detail-value">{vehicle.name}</span>
            </div>
            <div className="detail-item">
              <span className="detail-label">Тип транспортного средства:</span>
              <span className="detail-value">
                {vehicle.type ? (
                  <span className={`badge badge-${vehicle.type.toLowerCase()}`}>
                    {vehicle.type}
                  </span>
                ) : (
                  <em>Не указан</em>
                )}
              </span>
            </div>
            <div className="detail-item">
              <span className="detail-label">Дата создания:</span>
              <span className="detail-value">{formatDate(vehicle.creationDate)}</span>
            </div>
          </div>
        </div>

        {/* Координаты */}
        <div className="card">
          <div className="card-header">📍 Координаты</div>
          <div className="details-content">
            <div className="detail-item">
              <span className="detail-label">X:</span>
              <span className="detail-value">{vehicle.x}</span>
            </div>
            <div className="detail-item">
              <span className="detail-label">Y:</span>
              <span className="detail-value">{vehicle.y}</span>
            </div>
          </div>
        </div>

        {/* Характеристики */}
        <div className="card">
          <div className="card-header">⚙️ Характеристики</div>
          <div className="details-content">
            <div className="detail-item">
              <span className="detail-label">Мощность двигателя:</span>
              <span className="detail-value">{vehicle.enginePower} л.с.</span>
            </div>
            <div className="detail-item">
              <span className="detail-label">Количество колес:</span>
              <span className="detail-value">{vehicle.numberOfWheels}</span>
            </div>
            <div className="detail-item">
              <span className="detail-label">Вместимость:</span>
              <span className="detail-value">{vehicle.capacity}</span>
            </div>
          </div>
        </div>

        {/* Эксплуатация */}
        <div className="card">
          <div className="card-header">🛣️ Эксплуатация</div>
          <div className="details-content">
            <div className="detail-item">
              <span className="detail-label">Пройденное расстояние:</span>
              <span className="detail-value">{vehicle.distanceTravelled} км</span>
            </div>
            <div className="detail-item">
              <span className="detail-label">Расход топлива:</span>
              <span className="detail-value">{vehicle.fuelConsumption} л/100км</span>
            </div>
            <div className="detail-item">
              <span className="detail-label">Тип топлива:</span>
              <span className="detail-value">
                {vehicle.fuelType || <em>Не указан</em>}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div className="details-footer">
        <Link to="/vehicles" className="btn btn-secondary">
          ← Вернуться к списку
        </Link>
      </div>
    </div>
  );
};

export default VehicleDetails;






