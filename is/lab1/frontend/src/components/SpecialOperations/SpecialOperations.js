import React, { useState } from 'react';
import { vehicleService } from '../../services/api';
import './SpecialOperations.css';

const SpecialOperations = () => {
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState(null);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Для операций с параметрами
  const [namePrefix, setNamePrefix] = useState('');
  const [minFuelConsumption, setMinFuelConsumption] = useState('');
  const [vehicleType, setVehicleType] = useState('');
  const [resetVehicleId, setResetVehicleId] = useState('');

  const vehicleTypes = ['CAR', 'HELICOPTER', 'BOAT', 'HOVERBOARD'];

  // Обработчик для предотвращения изменения значения числовых полей при прокрутке колёсика мыши
  const handleWheel = (e) => {
    e.target.blur();
  };

  const clearMessages = () => {
    setError(null);
    setSuccess(null);
  };

  // 1. Vehicle с максимальной capacity
  const handleMaxCapacity = async () => {
    try {
      setLoading(true);
      clearMessages();
      setResults(null);

      const response = await vehicleService.getMaxCapacity();
      setResults({ type: 'single', data: response.data });
      setSuccess('Vehicle с максимальной вместимостью найден');
    } catch (err) {
      setError(err.message || 'Не удалось найти Vehicle');
    } finally {
      setLoading(false);
    }
  };

  // 2. Vehicle по префиксу имени
  const handleNamePrefix = async (e) => {
    e.preventDefault();
    if (!namePrefix) {
      setError('Введите префикс имени');
      return;
    }

    try {
      setLoading(true);
      clearMessages();
      setResults(null);

      const response = await vehicleService.getByNamePrefix(namePrefix);
      setResults({ type: 'list', data: response.data });
      setSuccess(`Найдено ${response.data.length} транспортных средств`);
    } catch (err) {
      setError(err.message || 'Не удалось найти Vehicle');
    } finally {
      setLoading(false);
    }
  };

  // 3. Vehicle по расходу топлива
  const handleFuelConsumption = async (e) => {
    e.preventDefault();
    if (!minFuelConsumption || minFuelConsumption < 0) {
      setError('Введите корректное значение расхода топлива');
      return;
    }

    try {
      setLoading(true);
      clearMessages();
      setResults(null);

      const response = await vehicleService.getByFuelConsumption(minFuelConsumption);
      setResults({ type: 'list', data: response.data });
      setSuccess(`Найдено ${response.data.length} транспортных средств`);
    } catch (err) {
      setError(err.message || 'Не удалось найти Vehicle');
    } finally {
      setLoading(false);
    }
  };

  // 4. Vehicle по типу
  const handleType = async (e) => {
    e.preventDefault();
    if (!vehicleType) {
      setError('Выберите тип транспортного средства');
      return;
    }

    try {
      setLoading(true);
      clearMessages();
      setResults(null);

      const response = await vehicleService.getByType(vehicleType);
      setResults({ type: 'list', data: response.data });
      setSuccess(`Найдено ${response.data.length} транспортных средств`);
    } catch (err) {
      setError(err.message || 'Не удалось найти Vehicle');
    } finally {
      setLoading(false);
    }
  };

  // 5. Сбросить пробег
  const handleResetDistance = async (e) => {
    e.preventDefault();
    if (!resetVehicleId) {
      setError('Введите ID транспортного средства');
      return;
    }

    if (!window.confirm(`Вы уверены, что хотите сбросить пробег для Vehicle с ID ${resetVehicleId}?`)) {
      return;
    }

    try {
      setLoading(true);
      clearMessages();
      setResults(null);

      await vehicleService.resetDistance(resetVehicleId);
      setSuccess(`Пробег для Vehicle с ID ${resetVehicleId} успешно сброшен до нуля`);
      setResetVehicleId('');
    } catch (err) {
      setError(err.message || 'Не удалось сбросить пробег');
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (date) => {
    if (!date) return '-';
    
    try {
      let parsedDate;
      
      // Если дата уже объект Date
      if (date instanceof Date) {
        parsedDate = date;
      }
      // Если это число (timestamp в миллисекундах)
      else if (typeof date === 'number') {
        parsedDate = new Date(date);
      }
      // Если это строка
      else if (typeof date === 'string') {
        // Пробуем парсить как ISO строку или другой формат
        parsedDate = new Date(date);
        
        // Если не получилось, пробуем как timestamp
        if (isNaN(parsedDate.getTime())) {
          const timestamp = parseInt(date, 10);
          if (!isNaN(timestamp)) {
            parsedDate = new Date(timestamp);
          }
        }
      }
      // Если это объект с полями даты
      else if (typeof date === 'object') {
        // Возможно, это объект вида {year: 2025, month: 11, day: 29, ...}
        parsedDate = new Date(date.year, date.month - 1, date.day, 
                              date.hour || 0, date.minute || 0, date.second || 0);
      }
      else {
        parsedDate = new Date(date);
      }
      
      // Проверяем, что дата валидна
      if (isNaN(parsedDate.getTime())) {
        console.error('Invalid date value:', date);
        return 'Invalid Date';
      }
      
      return parsedDate.toLocaleString('ru-RU', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      });
    } catch (e) {
      console.error('Error parsing date:', date, 'Type:', typeof date, 'Error:', e);
      return 'Invalid Date';
    }
  };

  return (
    <div className="special-operations">
      <h1 className="page-title">⚙️ Специальные операции</h1>
      <p className="page-subtitle">Выполнение дополнительных операций над транспортными средствами</p>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <div className="operations-grid">
        {/* 1. Максимальная capacity */}
        <div className="card">
          <div className="card-header">📊 Vehicle с максимальной вместимостью</div>
          <div className="operation-content">
            <p>Найти транспортное средство с максимальным значением поля capacity</p>
            <button
              onClick={handleMaxCapacity}
              disabled={loading}
              className="btn btn-primary"
            >
              Найти
            </button>
          </div>
        </div>

        {/* 2. Префикс имени */}
        <div className="card">
          <div className="card-header">🔤 Поиск по префиксу имени</div>
          <form onSubmit={handleNamePrefix} className="operation-content">
            <p>Найти все Vehicle, название которых начинается с заданной подстроки</p>
            <input
              type="text"
              value={namePrefix}
              onChange={(e) => setNamePrefix(e.target.value)}
              placeholder="Например: Tesla"
              className="form-control"
            />
            <button type="submit" disabled={loading} className="btn btn-primary">
              Найти
            </button>
          </form>
        </div>

        {/* 3. Расход топлива */}
        <div className="card">
          <div className="card-header">⛽ Поиск по расходу топлива</div>
          <form onSubmit={handleFuelConsumption} className="operation-content">
            <p>Найти все Vehicle с расходом топлива больше заданного</p>
            <input
              type="number"
              value={minFuelConsumption}
              onChange={(e) => setMinFuelConsumption(e.target.value)}
              onWheel={handleWheel}
              placeholder="Минимальный расход"
              min="0"
              className="form-control"
            />
            <button type="submit" disabled={loading} className="btn btn-primary">
              Найти
            </button>
          </form>
        </div>

        {/* 4. По типу */}
        <div className="card">
          <div className="card-header">🚗 Поиск по типу</div>
          <form onSubmit={handleType} className="operation-content">
            <p>Найти все транспортные средства заданного типа</p>
            <select
              value={vehicleType}
              onChange={(e) => setVehicleType(e.target.value)}
              className="form-control"
            >
              <option value="">-- Выберите тип --</option>
              {vehicleTypes.map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
              ))}
            </select>
            <button type="submit" disabled={loading} className="btn btn-primary">
              Найти
            </button>
          </form>
        </div>

        {/* 5. Сброс пробега */}
        <div className="card">
          <div className="card-header">🔄 Сброс пробега</div>
          <form onSubmit={handleResetDistance} className="operation-content">
            <p>Скрутить счётчик пробега транспортного средства до нуля</p>
            <input
              type="number"
              value={resetVehicleId}
              onChange={(e) => setResetVehicleId(e.target.value)}
              onWheel={handleWheel}
              placeholder="ID транспортного средства"
              min="1"
              className="form-control"
            />
            <button type="submit" disabled={loading} className="btn btn-danger">
              Сбросить пробег
            </button>
          </form>
        </div>
      </div>

      {/* Результаты */}
      {loading && (
        <div className="card">
          <div className="loading">
            <div className="loading-spinner"></div>
            <p>Выполнение операции...</p>
          </div>
        </div>
      )}

      {results && !loading && (
        <div className="card">
          <div className="card-header">📋 Результаты</div>
          <div className="results-content">
            {results.type === 'single' ? (
              <div className="result-item">
                <h3>{results.data.name}</h3>
                <div className="result-details">
                  <p><strong>ID:</strong> {results.data.id}</p>
                  <p><strong>Вместимость:</strong> {results.data.capacity}</p>
                  <p><strong>Тип:</strong> {results.data.type || '-'}</p>
                  <p><strong>Мощность:</strong> {results.data.enginePower} л.с.</p>
                  <p><strong>Колеса:</strong> {results.data.numberOfWheels}</p>
                  <p><strong>Координаты:</strong> X: {results.data.x}, Y: {results.data.y}</p>
                  <p><strong>Дата создания:</strong> {formatDate(results.data.creationDate)}</p>
                </div>
              </div>
            ) : (
              <div className="results-list">
                {results.data.length === 0 ? (
                  <p className="text-center">Ничего не найдено</p>
                ) : (
                  <div className="results-table">
                    <table className="table">
                      <thead>
                        <tr>
                          <th>ID</th>
                          <th>Название</th>
                          <th>Тип</th>
                          <th>Мощность</th>
                          <th>Вместимость</th>
                          <th>Пробег</th>
                          <th>Расход</th>
                        </tr>
                      </thead>
                      <tbody>
                        {results.data.map((vehicle) => (
                          <tr key={vehicle.id}>
                            <td>{vehicle.id}</td>
                            <td><strong>{vehicle.name}</strong></td>
                            <td>{vehicle.type || '-'}</td>
                            <td>{vehicle.enginePower}</td>
                            <td>{vehicle.capacity}</td>
                            <td>{vehicle.distanceTravelled}</td>
                            <td>{vehicle.fuelConsumption}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default SpecialOperations;






