import axios from 'axios';

const API_BASE_URL = '/project/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor для обработки ошибок
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.data) {
      const errorData = error.response.data;
      throw new Error(errorData.message || 'Произошла ошибка при выполнении запроса');
    }
    throw error;
  }
);

// CRUD операции для Vehicle
export const vehicleService = {
  // Получить все Vehicle с пагинацией и фильтрацией
  getAll: (params = {}) => {
    const { page = 0, size = 10, filterField, filterValue, sortField, sortDirection } = params;
    return api.get('/vehicles', { params: { page, size, filterField, filterValue, sortField, sortDirection } });
  },

  // Получить Vehicle по ID
  getById: (id) => {
    return api.get(`/vehicles/${id}`);
  },

  // Создать новый Vehicle
  create: (vehicleData) => {
    return api.post('/vehicles', vehicleData);
  },

  // Обновить Vehicle
  update: (id, vehicleData) => {
    return api.put(`/vehicles/${id}`, vehicleData);
  },

  // Удалить Vehicle
  delete: (id) => {
    return api.delete(`/vehicles/${id}`);
  },

  // Специальные операции
  getMaxCapacity: () => {
    return api.get('/vehicles/max-capacity');
  },

  getByNamePrefix: (prefix) => {
    return api.get('/vehicles/by-name-prefix', { params: { prefix } });
  },

  getByFuelConsumption: (minConsumption) => {
    return api.get('/vehicles/by-fuel-consumption', { params: { minConsumption } });
  },

  getByType: (type) => {
    return api.get('/vehicles/by-type', { params: { type } });
  },

  resetDistance: (id) => {
    return api.post(`/vehicles/${id}/reset-distance`);
  },
};

// CRUD операции для Coordinates
export const coordinatesService = {
  // Получить все координаты
  getAll: () => {
    return api.get('/coordinates');
  },

  // Получить координаты по ID
  getById: (id) => {
    return api.get(`/coordinates/${id}`);
  },
};

export default api;






