import React, { useState, useEffect, useCallback } from 'react';
import { importService } from '../../services/api';
import './Import.css';

const Import = () => {
  const [file, setFile] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [importing, setImporting] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  
  // Управление пользователем и ролью
  const [isAdmin, setIsAdmin] = useState(false);
  const [username, setUsername] = useState('user');

  const loadHistory = useCallback(async () => {
    setLoading(true);
    try {
      const response = await importService.getHistory(username, isAdmin);
      setHistory(response.data);
      setError(null);
    } catch (err) {
      setError('Ошибка загрузки истории: ' + err.message);
    } finally {
      setLoading(false);
    }
  }, [username, isAdmin]);

  useEffect(() => {
    loadHistory();
  }, [loadHistory]);

  const handleFileChange = (e) => {
    const selectedFile = e.target.files[0];
    if (selectedFile && selectedFile.type === 'application/json') {
      setFile(selectedFile);
      setError(null);
    } else {
      setFile(null);
      setError('Пожалуйста, выберите JSON файл');
    }
  };

  const handleImport = async () => {
    if (!file) {
      setError('Выберите файл для импорта');
      return;
    }

    setImporting(true);
    setError(null);
    setSuccess(null);

    try {
      const content = await file.text();
      const data = JSON.parse(content);
      
      if (!Array.isArray(data)) {
        throw new Error('JSON должен содержать массив объектов');
      }

      const response = await importService.importVehicles(data, username, isAdmin);
      
      if (response.data.status === 'SUCCESS') {
        setSuccess(`Импорт успешно завершен! Добавлено объектов: ${response.data.addedCount}`);
      } else {
        setError(`Ошибка импорта: ${response.data.errorMessage}`);
      }
      
      setFile(null);
      // Сброс input файла
      document.getElementById('file-input').value = '';
      
      // Обновляем историю
      await loadHistory();
      
    } catch (err) {
      if (err instanceof SyntaxError) {
        setError('Некорректный формат JSON файла');
      } else {
        setError('Ошибка импорта: ' + err.message);
      }
    } finally {
      setImporting(false);
    }
  };

  const toggleRole = () => {
    const newIsAdmin = !isAdmin;
    setIsAdmin(newIsAdmin);
    setUsername(newIsAdmin ? 'admin' : 'user');
  };

  const formatDate = (timestamp) => {
    if (!timestamp) return '-';
    return new Date(timestamp).toLocaleString('ru-RU');
  };

  const getStatusBadge = (status) => {
    const statusMap = {
      'SUCCESS': { class: 'status-success', text: 'Успешно' },
      'FAILED': { class: 'status-failed', text: 'Ошибка' },
      'IN_PROGRESS': { class: 'status-progress', text: 'В процессе' },
    };
    const statusInfo = statusMap[status] || { class: '', text: status };
    return <span className={`status-badge ${statusInfo.class}`}>{statusInfo.text}</span>;
  };

  return (
    <div className="import-container">
      <div className="import-header">
        <h2>📥 Импорт данных</h2>
        
        <div className="role-switcher">
          <span className="role-label">Текущий пользователь:</span>
          <button 
            className={`role-button ${isAdmin ? 'admin' : 'user'}`}
            onClick={toggleRole}
          >
            {isAdmin ? '👑 Администратор' : '👤 Пользователь'}
          </button>
          <span className="username-display">({username})</span>
        </div>
      </div>

      <div className="import-section">
        <div className="import-card">
          <h3>📁 Загрузка файла</h3>
          <p className="import-description">
            Выберите JSON файл с массивом объектов Vehicle для импорта.
            Все объекты будут добавлены в рамках одной транзакции.
          </p>
          
          <div className="file-upload-area">
            <input
              id="file-input"
              type="file"
              accept=".json,application/json"
              onChange={handleFileChange}
              className="file-input"
            />
            <label htmlFor="file-input" className="file-label">
              {file ? `📄 ${file.name}` : '📎 Выберите JSON файл'}
            </label>
          </div>

          <button 
            className="import-button"
            onClick={handleImport}
            disabled={!file || importing}
          >
            {importing ? '⏳ Импорт...' : '🚀 Импортировать'}
          </button>

          {error && <div className="alert alert-error">❌ {error}</div>}
          {success && <div className="alert alert-success">✅ {success}</div>}

          <div className="json-example">
            <h4>Пример JSON формата:</h4>
            <pre>{`[
  {
    "name": "Toyota Camry",
    "x": 10.5,
    "y": 200,
    "type": "CAR",
    "enginePower": 180,
    "numberOfWheels": 4,
    "capacity": 5.0,
    "distanceTravelled": 50000,
    "fuelConsumption": 8,
    "fuelType": "GASOLINE"
  }
]`}</pre>
          </div>
        </div>
      </div>

      <div className="history-section">
        <h3>📋 История импорта {isAdmin && <span className="admin-badge">(все операции)</span>}</h3>
        
        <button className="refresh-button" onClick={loadHistory} disabled={loading}>
          {loading ? '⏳ Загрузка...' : '🔄 Обновить'}
        </button>

        {history.length === 0 ? (
          <p className="no-history">История импорта пуста</p>
        ) : (
          <table className="history-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Статус</th>
                <th>Пользователь</th>
                <th>Добавлено</th>
                <th>Дата</th>
                <th>Ошибка</th>
              </tr>
            </thead>
            <tbody>
              {history.map((op) => (
                <tr key={op.id}>
                  <td>{op.id}</td>
                  <td>{getStatusBadge(op.status)}</td>
                  <td>{op.username}</td>
                  <td>{op.status === 'SUCCESS' ? op.addedCount : '-'}</td>
                  <td>{formatDate(op.createdAt)}</td>
                  <td className="error-cell" title={op.errorMessage}>
                    {op.errorMessage ? op.errorMessage.substring(0, 50) + '...' : '-'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="info-section">
        <h3>ℹ️ Информация об ограничениях</h3>
        <div className="constraints-info">
          <h4>Ограничения уникальности (проверяются на программном уровне):</h4>
          <ul>
            <li><strong>Name + Type:</strong> Нельзя создать два транспортных средства с одинаковым названием и типом</li>
            <li><strong>EnginePower + Capacity + FuelType:</strong> Уникальная техническая конфигурация транспортного средства</li>
          </ul>
          
          <h4>Ограничения полей:</h4>
          <ul>
            <li><strong>name:</strong> не может быть пустым</li>
            <li><strong>x, y:</strong> обязательные (y ≤ 621)</li>
            <li><strong>enginePower, numberOfWheels, fuelConsumption:</strong> должны быть больше 0</li>
            <li><strong>capacity:</strong> должна быть больше 0</li>
            <li><strong>distanceTravelled:</strong> не может быть отрицательным</li>
          </ul>
        </div>
      </div>
    </div>
  );
};

export default Import;

