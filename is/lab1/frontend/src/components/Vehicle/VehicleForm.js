import React, { useState, useEffect } from 'react';
import './VehicleForm.css';
import { coordinatesService } from '../../services/api';

const VehicleForm = ({ formData, errors, onChange, onSubmit, isEdit }) => {
  const vehicleTypes = ['CAR', 'HELICOPTER', 'BOAT', 'HOVERBOARD'];
  const fuelTypes = ['KEROSENE', 'ELECTRICITY', 'DIESEL', 'ALCOHOL'];
  
  const [useExistingCoordinates, setUseExistingCoordinates] = useState(false);
  const [availableCoordinates, setAvailableCoordinates] = useState([]);
  const [loadingCoordinates, setLoadingCoordinates] = useState(false);

  // Обработчик для предотвращения изменения значения числовых полей при прокрутке колёсика мыши
  const handleWheel = (e) => {
    // Убираем фокус с поля, чтобы предотвратить изменение значения
    e.target.blur();
  };

  // Загрузка существующих координат
  useEffect(() => {
    const loadCoordinates = async () => {
      try {
        setLoadingCoordinates(true);
        const response = await coordinatesService.getAll();
        setAvailableCoordinates(response.data);
      } catch (error) {
        console.error('Ошибка загрузки координат:', error);
      } finally {
        setLoadingCoordinates(false);
      }
    };
    loadCoordinates();
  }, []);

  const handleCoordinatesModeChange = (e) => {
    const useExisting = e.target.value === 'existing';
    setUseExistingCoordinates(useExisting);
    
    // Очищаем поля координат при переключении
    if (useExisting) {
      onChange({ target: { name: 'x', value: '' } });
      onChange({ target: { name: 'y', value: '' } });
    } else {
      onChange({ target: { name: 'coordinatesId', value: '' } });
    }
  };

  return (
    <form onSubmit={onSubmit} className="vehicle-form">
      {/* Название */}
      <div className="form-group">
        <label htmlFor="name">
          Название <span className="required">*</span>
        </label>
        <input
          type="text"
          id="name"
          name="name"
          value={formData.name}
          onChange={onChange}
          className={`form-control ${errors.name ? 'is-invalid' : ''}`}
          placeholder="Например: Tesla Model S"
          required
        />
        {errors.name && <div className="error-message">{errors.name}</div>}
      </div>

      {/* Выбор способа указания координат */}
      <div className="form-group">
        <label>
          Координаты <span className="required">*</span>
        </label>
        <div className="radio-group">
          <label className="radio-label">
            <input
              type="radio"
              name="coordinatesMode"
              value="new"
              checked={!useExistingCoordinates}
              onChange={handleCoordinatesModeChange}
            />
            <span>Создать новые координаты</span>
          </label>
          <label className="radio-label">
            <input
              type="radio"
              name="coordinatesMode"
              value="existing"
              checked={useExistingCoordinates}
              onChange={handleCoordinatesModeChange}
            />
            <span>Использовать существующие координаты</span>
          </label>
        </div>
      </div>

      {/* Выбор существующих координат */}
      {useExistingCoordinates ? (
        <div className="form-group">
          <label htmlFor="coordinatesId">
            Выберите координаты <span className="required">*</span>
          </label>
          <select
            id="coordinatesId"
            name="coordinatesId"
            value={formData.coordinatesId || ''}
            onChange={onChange}
            className={`form-control ${errors.coordinatesId ? 'is-invalid' : ''}`}
            required
            disabled={loadingCoordinates}
          >
            <option value="">-- Выберите координаты --</option>
            {availableCoordinates.map((coord) => (
              <option key={coord.id} value={coord.id}>
                ID: {coord.id} - X: {coord.x}, Y: {coord.y}
              </option>
            ))}
          </select>
          {loadingCoordinates && <small>Загрузка координат...</small>}
          {errors.coordinatesId && <div className="error-message">{errors.coordinatesId}</div>}
        </div>
      ) : (
        /* Ввод новых координат */
        <div className="form-row">
          <div className="form-group">
            <label htmlFor="x">
              Координата X <span className="required">*</span>
            </label>
            <input
              type="number"
              id="x"
              name="x"
              value={formData.x || ''}
              onChange={onChange}
              onWheel={handleWheel}
              step="0.01"
              className={`form-control ${errors.x ? 'is-invalid' : ''}`}
              required={!useExistingCoordinates}
            />
            {errors.x && <div className="error-message">{errors.x}</div>}
          </div>

          <div className="form-group">
            <label htmlFor="y">
              Координата Y <span className="required">*</span>
              <small> (макс. 621)</small>
            </label>
            <input
              type="number"
              id="y"
              name="y"
              value={formData.y || ''}
              onChange={onChange}
              onWheel={handleWheel}
              max="621"
              className={`form-control ${errors.y ? 'is-invalid' : ''}`}
              required={!useExistingCoordinates}
            />
            {errors.y && <div className="error-message">{errors.y}</div>}
          </div>
        </div>
      )}

      {/* Тип транспорта */}
      <div className="form-group">
        <label htmlFor="type">
          Тип транспортного средства <span className="required">*</span>
        </label>
        <select
          id="type"
          name="type"
          value={formData.type}
          onChange={onChange}
          className={`form-control ${errors.type ? 'is-invalid' : ''}`}
          required
        >
          <option value="">-- Выберите тип --</option>
          {vehicleTypes.map((type) => (
            <option key={type} value={type}>
              {type}
            </option>
          ))}
        </select>
        {errors.type && <div className="error-message">{errors.type}</div>}
      </div>

      {/* Мощность двигателя и колеса */}
      <div className="form-row">
        <div className="form-group">
          <label htmlFor="enginePower">
            Мощность двигателя <span className="required">*</span>
          </label>
          <input
            type="number"
            id="enginePower"
            name="enginePower"
            value={formData.enginePower}
            onChange={onChange}
            onWheel={handleWheel}
            min="1"
            className={`form-control ${errors.enginePower ? 'is-invalid' : ''}`}
            required
          />
          {errors.enginePower && <div className="error-message">{errors.enginePower}</div>}
        </div>

        <div className="form-group">
          <label htmlFor="numberOfWheels">
            Количество колес <span className="required">*</span>
          </label>
          <input
            type="number"
            id="numberOfWheels"
            name="numberOfWheels"
            value={formData.numberOfWheels}
            onChange={onChange}
            onWheel={handleWheel}
            min="1"
            className={`form-control ${errors.numberOfWheels ? 'is-invalid' : ''}`}
            required
          />
          {errors.numberOfWheels && <div className="error-message">{errors.numberOfWheels}</div>}
        </div>
      </div>

      {/* Вместимость */}
      <div className="form-group">
        <label htmlFor="capacity">
          Вместимость <span className="required">*</span>
        </label>
        <input
          type="number"
          id="capacity"
          name="capacity"
          value={formData.capacity}
          onChange={onChange}
          onWheel={handleWheel}
          min="0.01"
          step="0.01"
          className={`form-control ${errors.capacity ? 'is-invalid' : ''}`}
          required
        />
        {errors.capacity && <div className="error-message">{errors.capacity}</div>}
      </div>

      {/* Пробег и расход топлива */}
      <div className="form-row">
        <div className="form-group">
          <label htmlFor="distanceTravelled">
            Пройденное расстояние <span className="required">*</span>
          </label>
          <input
            type="number"
            id="distanceTravelled"
            name="distanceTravelled"
            value={formData.distanceTravelled}
            onChange={onChange}
            onWheel={handleWheel}
            min="0"
            className={`form-control ${errors.distanceTravelled ? 'is-invalid' : ''}`}
            required
          />
          {errors.distanceTravelled && <div className="error-message">{errors.distanceTravelled}</div>}
        </div>

        <div className="form-group">
          <label htmlFor="fuelConsumption">
            Расход топлива <span className="required">*</span>
          </label>
          <input
            type="number"
            id="fuelConsumption"
            name="fuelConsumption"
            value={formData.fuelConsumption}
            onChange={onChange}
            onWheel={handleWheel}
            min="1"
            className={`form-control ${errors.fuelConsumption ? 'is-invalid' : ''}`}
            required
          />
          {errors.fuelConsumption && <div className="error-message">{errors.fuelConsumption}</div>}
        </div>
      </div>

      {/* Тип топлива */}
      <div className="form-group">
        <label htmlFor="fuelType">
          Тип топлива <span className="required">*</span>
        </label>
        <select
          id="fuelType"
          name="fuelType"
          value={formData.fuelType}
          onChange={onChange}
          className={`form-control ${errors.fuelType ? 'is-invalid' : ''}`}
          required
        >
          <option value="">-- Выберите тип топлива --</option>
          {fuelTypes.map((type) => (
            <option key={type} value={type}>
              {type}
            </option>
          ))}
        </select>
        {errors.fuelType && <div className="error-message">{errors.fuelType}</div>}
      </div>

      {/* Кнопки */}
      <div className="form-actions">
        <button type="submit" className="btn btn-primary btn-lg">
          {isEdit ? '💾 Сохранить изменения' : '➕ Создать Vehicle'}
        </button>
        <button
          type="button"
          onClick={() => window.history.back()}
          className="btn btn-secondary btn-lg"
        >
          ❌ Отмена
        </button>
      </div>

      <p className="form-note">
        <span className="required">*</span> - обязательные поля
      </p>
    </form>
  );
};

export default VehicleForm;






