import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { vehicleService } from '../../services/api';
import './VehicleList.css';

const VehicleList = () => {
  const [vehicles, setVehicles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Пагинация
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Фильтрация и сортировка
  const [filterField, setFilterField] = useState('');
  const [filterValue, setFilterValue] = useState('');
  const [sortField, setSortField] = useState('id');
  const [sortDirection, setSortDirection] = useState('asc');

  useEffect(() => {
    loadVehicles();
  }, [currentPage, pageSize, sortField, sortDirection]);

  const loadVehicles = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const params = {
        page: currentPage,
        size: pageSize,
        sortField,
        sortDirection,
      };

      if (filterField && filterValue) {
        params.filterField = filterField;
        params.filterValue = filterValue;
      }

      const response = await vehicleService.getAll(params);
      const data = response.data;

      setVehicles(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      setError(err.message || 'Не удалось загрузить список транспортных средств');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Вы уверены, что хотите удалить это транспортное средство?')) {
      return;
    }

    try {
      await vehicleService.delete(id);
      setSuccess('Транспортное средство успешно удалено');
      loadVehicles();
      setTimeout(() => setSuccess(null), 3000);
    } catch (err) {
      setError(err.message || 'Не удалось удалить транспортное средство');
    }
  };

  const handleFilter = (e) => {
    e.preventDefault();
    setCurrentPage(0);
    loadVehicles();
  };

  const handleClearFilter = () => {
    setFilterField('');
    setFilterValue('');
    setCurrentPage(0);
    setTimeout(loadVehicles, 0);
  };

  const handleSort = (field) => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('asc');
    }
  };

  const formatDate = (date) => {
    if (!date) return '-';
    
    try {
      console.log('Received date:', date, 'Type:', typeof date);
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
      
      console.log('Parsed date:', parsedDate, 'IsValid:', !isNaN(parsedDate.getTime()));
      
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

  if (loading && vehicles.length === 0) {
    return (
      <div className="loading">
        <div className="loading-spinner"></div>
        <p>Загрузка...</p>
      </div>
    );
  }

  return (
    <div className="vehicle-list">
      <div className="page-header flex-between">
        <div>
          <h1 className="page-title">Все транспортные средства</h1>
          <p className="page-subtitle">Всего записей: {totalElements}</p>
        </div>
        <Link to="/vehicles/create" className="btn btn-primary btn-lg">
          ➕ Создать Vehicle
        </Link>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      {/* Фильтрация */}
      <div className="card">
        <div className="card-header">🔍 Фильтрация и сортировка</div>
        <form onSubmit={handleFilter} className="filter-form">
          <div className="filter-row">
            <div className="form-group">
              <label>Поле для фильтрации:</label>
              <select
                value={filterField}
                onChange={(e) => setFilterField(e.target.value)}
                className="form-control"
              >
                <option value="">-- Выберите поле --</option>
                <option value="name">Название</option>
                <option value="type">Тип</option>
                <option value="fuelType">Тип топлива</option>
              </select>
            </div>

            <div className="form-group">
              <label>Значение фильтра:</label>
              <input
                type="text"
                value={filterValue}
                onChange={(e) => setFilterValue(e.target.value)}
                placeholder="Введите значение для поиска..."
                className="form-control"
                disabled={!filterField}
              />
            </div>

            <div className="form-group">
              <label>Размер страницы:</label>
              <select
                value={pageSize}
                onChange={(e) => {
                  setPageSize(Number(e.target.value));
                  setCurrentPage(0);
                }}
                className="form-control"
              >
                <option value="5">5</option>
                <option value="10">10</option>
                <option value="20">20</option>
                <option value="50">50</option>
              </select>
            </div>
          </div>

          <div className="filter-actions">
            <button type="submit" className="btn btn-primary" disabled={!filterField || !filterValue}>
              Применить фильтр
            </button>
            <button type="button" onClick={handleClearFilter} className="btn btn-secondary">
              Очистить
            </button>
          </div>
        </form>
      </div>

      {/* Таблица */}
      <div className="card">
        <div className="table-responsive">
          <table className="table">
            <thead>
              <tr>
                <th onClick={() => handleSort('id')} className="sortable">
                  ID {sortField === 'id' && (sortDirection === 'asc' ? '↑' : '↓')}
                </th>
                <th onClick={() => handleSort('name')} className="sortable">
                  Название {sortField === 'name' && (sortDirection === 'asc' ? '↑' : '↓')}
                </th>
                <th>Координаты</th>
                <th onClick={() => handleSort('type')} className="sortable">
                  Тип {sortField === 'type' && (sortDirection === 'asc' ? '↑' : '↓')}
                </th>
                <th onClick={() => handleSort('enginePower')} className="sortable">
                  Мощность {sortField === 'enginePower' && (sortDirection === 'asc' ? '↑' : '↓')}
                </th>
                <th onClick={() => handleSort('numberOfWheels')} className="sortable">
                  Колеса {sortField === 'numberOfWheels' && (sortDirection === 'asc' ? '↑' : '↓')}
                </th>
                <th onClick={() => handleSort('capacity')} className="sortable">
                  Вместимость {sortField === 'capacity' && (sortDirection === 'asc' ? '↑' : '↓')}
                </th>
                <th onClick={() => handleSort('distanceTravelled')} className="sortable">
                  Пробег {sortField === 'distanceTravelled' && (sortDirection === 'asc' ? '↑' : '↓')}
                </th>
                <th onClick={() => handleSort('fuelConsumption')} className="sortable">
                  Расход {sortField === 'fuelConsumption' && (sortDirection === 'asc' ? '↑' : '↓')}
                </th>
                <th>Топливо</th>
                <th onClick={() => handleSort('creationDate')} className="sortable">
                  Дата создания {sortField === 'creationDate' && (sortDirection === 'asc' ? '↑' : '↓')}
                </th>
                <th>Действия</th>
              </tr>
            </thead>
            <tbody>
              {vehicles.length === 0 ? (
                <tr>
                  <td colSpan="12" className="text-center">
                    Нет данных для отображения
                  </td>
                </tr>
              ) : (
                vehicles.map((vehicle) => (
                  <tr key={vehicle.id}>
                    <td>{vehicle.id}</td>
                    <td><strong>{vehicle.name}</strong></td>
                    <td>
                      <small>
                        X: {vehicle.x}<br />
                        Y: {vehicle.y}
                      </small>
                    </td>
                    <td>
                      <span className={`badge badge-${vehicle.type?.toLowerCase()}`}>
                        {vehicle.type || '-'}
                      </span>
                    </td>
                    <td>{vehicle.enginePower}</td>
                    <td>{vehicle.numberOfWheels}</td>
                    <td>{vehicle.capacity}</td>
                    <td>{vehicle.distanceTravelled}</td>
                    <td>{vehicle.fuelConsumption}</td>
                    <td>{vehicle.fuelType || '-'}</td>
                    <td><small>{formatDate(vehicle.creationDate)}</small></td>
                    <td>
                      <div className="action-buttons">
                        <Link
                          to={`/vehicles/${vehicle.id}`}
                          className="btn btn-info btn-sm"
                          title="Подробнее"
                        >
                          👁️
                        </Link>
                        <Link
                          to={`/vehicles/${vehicle.id}/edit`}
                          className="btn btn-warning btn-sm"
                          title="Редактировать"
                        >
                          ✏️
                        </Link>
                        <button
                          onClick={() => handleDelete(vehicle.id)}
                          className="btn btn-danger btn-sm"
                          title="Удалить"
                        >
                          🗑️
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Пагинация */}
        {totalPages > 1 && (
          <div className="pagination">
            <button
              onClick={() => setCurrentPage(0)}
              disabled={currentPage === 0}
              className="btn btn-secondary btn-sm"
            >
              ⏮️ Первая
            </button>
            <button
              onClick={() => setCurrentPage(currentPage - 1)}
              disabled={currentPage === 0}
              className="btn btn-secondary btn-sm"
            >
              ⬅️ Предыдущая
            </button>
            <span className="pagination-info">
              Страница {currentPage + 1} из {totalPages}
            </span>
            <button
              onClick={() => setCurrentPage(currentPage + 1)}
              disabled={currentPage >= totalPages - 1}
              className="btn btn-secondary btn-sm"
            >
              Следующая ➡️
            </button>
            <button
              onClick={() => setCurrentPage(totalPages - 1)}
              disabled={currentPage >= totalPages - 1}
              className="btn btn-secondary btn-sm"
            >
              Последняя ⏭️
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default VehicleList;






