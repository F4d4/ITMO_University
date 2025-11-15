import React from 'react';
import './VehicleForm.css';

const VehicleForm = ({ formData, errors, onChange, onSubmit, isEdit }) => {
  const vehicleTypes = ['CAR', 'HELICOPTER', 'BOAT', 'HOVERBOARD'];
  const fuelTypes = ['KEROSENE', 'ELECTRICITY', 'DIESEL', 'ALCOHOL'];

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

      {/* Координаты */}
      <div className="form-row">
        <div className="form-group">
          <label htmlFor="x">
            Координата X <span className="required">*</span>
          </label>
          <input
            type="number"
            id="x"
            name="x"
            value={formData.x}
            onChange={onChange}
            step="0.01"
            className={`form-control ${errors.x ? 'is-invalid' : ''}`}
            required
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
            value={formData.y}
            onChange={onChange}
            max="621"
            className={`form-control ${errors.y ? 'is-invalid' : ''}`}
            required
          />
          {errors.y && <div className="error-message">{errors.y}</div>}
        </div>
      </div>

      {/* Тип транспорта */}
      <div className="form-group">
        <label htmlFor="type">Тип транспортного средства</label>
        <select
          id="type"
          name="type"
          value={formData.type}
          onChange={onChange}
          className="form-control"
        >
          <option value="">-- Не выбрано --</option>
          {vehicleTypes.map((type) => (
            <option key={type} value={type}>
              {type}
            </option>
          ))}
        </select>
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
            min="1"
            className={`form-control ${errors.fuelConsumption ? 'is-invalid' : ''}`}
            required
          />
          {errors.fuelConsumption && <div className="error-message">{errors.fuelConsumption}</div>}
        </div>
      </div>

      {/* Тип топлива */}
      <div className="form-group">
        <label htmlFor="fuelType">Тип топлива</label>
        <select
          id="fuelType"
          name="fuelType"
          value={formData.fuelType}
          onChange={onChange}
          className="form-control"
        >
          <option value="">-- Не выбрано --</option>
          {fuelTypes.map((type) => (
            <option key={type} value={type}>
              {type}
            </option>
          ))}
        </select>
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

