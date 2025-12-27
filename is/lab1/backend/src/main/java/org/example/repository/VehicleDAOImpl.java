package org.example.repository;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.example.config.HibernateUtil;
import org.example.entity.Vehicle;
import org.example.entity.VehicleType;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Реализация DAO для работы с Vehicle через Hibernate Session API
 * Использует ручное управление сессиями и транзакциями
 */
@Stateless
public class VehicleDAOImpl implements VehicleDAO {

    private static final Logger LOGGER = Logger.getLogger(VehicleDAOImpl.class.getName());

    @Inject
    private HibernateUtil hibernateUtil;

    @Override
    public Vehicle save(Vehicle vehicle) {
        Session session = null;
        Transaction tx = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            
            session.persist(vehicle);
            
            tx.commit();
            LOGGER.info("Vehicle успешно сохранен с ID: " + vehicle.getId());
            return vehicle;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
                LOGGER.warning("Транзакция откачена");
            }
            LOGGER.severe("Ошибка при сохранении Vehicle: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось сохранить Vehicle", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public Optional<Vehicle> findById(Integer id) {
        Session session = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            Vehicle vehicle = session.get(Vehicle.class, id);
            return Optional.ofNullable(vehicle);
        } catch (Exception e) {
            LOGGER.severe("Ошибка при поиске Vehicle по ID " + id + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось найти Vehicle по ID", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public Optional<Vehicle> findByIdWithLock(Integer id) {
        Session session = null;
        Transaction tx = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            
            // Пессимистическая блокировка - FOR UPDATE
            Vehicle vehicle = session.createQuery(
                    "FROM Vehicle v WHERE v.id = :id", Vehicle.class)
                    .setParameter("id", id)
                    .setLockMode(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
                    .uniqueResult();
            
            tx.commit();
            LOGGER.info("Получена пессимистическая блокировка для Vehicle ID: " + id);
            
            return Optional.ofNullable(vehicle);
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            LOGGER.severe("Ошибка при получении блокировки для Vehicle ID " + id + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось получить блокировку Vehicle", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public List<Vehicle> findAll(int page, int size) {
        Session session = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            Query<Vehicle> query = session.createQuery(
                    "FROM Vehicle v ORDER BY v.id", Vehicle.class);
            query.setFirstResult(page * size);
            query.setMaxResults(size);
            // Включаем query cache для кэширования результатов запроса
            query.setCacheable(true);
            query.setCacheRegion("vehicle-list-cache");
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при получении списка Vehicle: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось получить список Vehicle. Проверьте подключение к базе данных", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public long count() {
        Session session = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            Query<Long> query = session.createQuery(
                    "SELECT COUNT(v) FROM Vehicle v", Long.class);
            // Включаем кэширование для запроса подсчета
            query.setCacheable(true);
            query.setCacheRegion("vehicle-count-cache");
            return query.getSingleResult();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при подсчете Vehicle: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось подсчитать количество Vehicle", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public Vehicle update(Vehicle vehicle) {
        Session session = null;
        Transaction tx = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            
            Vehicle merged = session.merge(vehicle);
            
            tx.commit();
            LOGGER.info("Vehicle успешно обновлен с ID: " + vehicle.getId());
            return merged;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            LOGGER.severe("Ошибка при обновлении Vehicle: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось обновить Vehicle", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public Vehicle updateWithLock(Integer id, java.util.function.Consumer<Vehicle> updater) {
        Session session = null;
        Transaction tx = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            
            // Устанавливаем уровень изоляции SERIALIZABLE
            session.doWork(connection -> {
                connection.setTransactionIsolation(java.sql.Connection.TRANSACTION_SERIALIZABLE);
            });
            
            // Получаем объект с пессимистической блокировкой (FOR UPDATE)
            Vehicle vehicle = session.createQuery(
                    "FROM Vehicle v WHERE v.id = :id", Vehicle.class)
                    .setParameter("id", id)
                    .setLockMode(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
                    .uniqueResult();
            
            if (vehicle == null) {
                throw new IllegalArgumentException("Vehicle с ID " + id + " не найден");
            }
            
            LOGGER.info("Получена блокировка для Vehicle ID: " + id);
            
            // Применяем обновления
            updater.accept(vehicle);
            
            // Сохраняем изменения
            session.merge(vehicle);
            
            tx.commit();
            LOGGER.info("Vehicle успешно обновлен с блокировкой, ID: " + id);
            return vehicle;
        } catch (IllegalArgumentException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            LOGGER.severe("Ошибка при обновлении Vehicle с блокировкой: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось обновить Vehicle", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public void deleteByIdWithLock(Integer id) {
        Session session = null;
        Transaction tx = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            
            // Получаем объект с пессимистической блокировкой
            Vehicle vehicle = session.createQuery(
                    "FROM Vehicle v WHERE v.id = :id", Vehicle.class)
                    .setParameter("id", id)
                    .setLockMode(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
                    .uniqueResult();
            
            if (vehicle == null) {
                throw new IllegalArgumentException("Vehicle с ID " + id + " не найден");
            }
            
            LOGGER.info("Получена блокировка для удаления Vehicle ID: " + id);
            
            session.remove(vehicle);
            
            tx.commit();
            LOGGER.info("Vehicle успешно удален с блокировкой, ID: " + id);
        } catch (IllegalArgumentException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            LOGGER.severe("Ошибка при удалении Vehicle с блокировкой: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось удалить Vehicle", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public Vehicle saveWithUniquenessCheck(Vehicle vehicle, java.util.function.Consumer<Vehicle> uniquenessChecker) {
        Session session = null;
        Transaction tx = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            
            // Устанавливаем уровень изоляции SERIALIZABLE для предотвращения race condition
            session.doWork(connection -> {
                connection.setTransactionIsolation(java.sql.Connection.TRANSACTION_SERIALIZABLE);
            });
            
            // Проверяем уникальность в рамках этой же транзакции
            uniquenessChecker.accept(vehicle);
            
            // Сохраняем объект
            session.persist(vehicle);
            
            tx.commit();
            LOGGER.info("Vehicle успешно сохранен с проверкой уникальности, ID: " + vehicle.getId());
            return vehicle;
        } catch (IllegalArgumentException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            LOGGER.severe("Ошибка при сохранении Vehicle с проверкой уникальности: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось сохранить Vehicle", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public void deleteById(Integer id) {
        Session session = null;
        Transaction tx = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            
            Vehicle vehicle = session.get(Vehicle.class, id);
            if (vehicle != null) {
                session.remove(vehicle);
                tx.commit();
                LOGGER.info("Vehicle успешно удален с ID: " + id);
            } else {
                throw new IllegalArgumentException("Vehicle с ID " + id + " не найден");
            }
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            LOGGER.severe("Ошибка при удалении Vehicle: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось удалить Vehicle", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public Optional<Vehicle> findByMaxCapacity() {
        Session session = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            Query<Vehicle> query = session.createQuery(
                    "FROM Vehicle v WHERE v.capacity = (SELECT MAX(v2.capacity) FROM Vehicle v2)",
                    Vehicle.class);
            query.setMaxResults(1);
            List<Vehicle> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            LOGGER.severe("Ошибка при поиске Vehicle с максимальной capacity: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось найти Vehicle с максимальной capacity", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public List<Vehicle> findByNameStartsWith(String prefix) {
        Session session = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            Query<Vehicle> query = session.createQuery(
                    "FROM Vehicle v WHERE v.name LIKE :prefix",
                    Vehicle.class);
            query.setParameter("prefix", prefix + "%");
            query.setCacheable(true);
            query.setCacheRegion("vehicle-query-cache");
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при поиске Vehicle по префиксу имени: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось найти Vehicle по префиксу имени", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public List<Vehicle> findByFuelConsumptionGreaterThan(long fuelConsumption) {
        Session session = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            Query<Vehicle> query = session.createQuery(
                    "FROM Vehicle v WHERE v.fuelConsumption > :consumption",
                    Vehicle.class);
            query.setParameter("consumption", fuelConsumption);
            query.setCacheable(true);
            query.setCacheRegion("vehicle-query-cache");
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при поиске Vehicle по fuelConsumption: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось найти Vehicle по fuelConsumption", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public List<Vehicle> findByType(VehicleType type) {
        Session session = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            Query<Vehicle> query = session.createQuery(
                    "FROM Vehicle v WHERE v.type = :type",
                    Vehicle.class);
            query.setParameter("type", type);
            query.setCacheable(true);
            query.setCacheRegion("vehicle-query-cache");
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при поиске Vehicle по типу: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось найти Vehicle по типу", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public void resetDistanceTravelled(Integer id) {
        Session session = null;
        Transaction tx = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            
            Vehicle vehicle = session.get(Vehicle.class, id);
            if (vehicle != null) {
                vehicle.setDistanceTravelled(0);
                session.merge(vehicle);
                tx.commit();
                LOGGER.info("Пробег успешно скручен для Vehicle с ID: " + id);
            } else {
                throw new IllegalArgumentException("Vehicle с ID " + id + " не найден");
            }
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            LOGGER.severe("Ошибка при сбросе пробега Vehicle: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось сбросить пробег Vehicle", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public List<Vehicle> findWithFilters(String filterField, String filterValue,
            String sortField, String sortDirection,
            int page, int size) {
        Session session = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();

            boolean hasFilter = filterField != null && !filterField.isEmpty() &&
                    filterValue != null && !filterValue.isEmpty();
            
            // Для enum полей (type, fuelType) используем фильтрацию в памяти
            // так как они могут храниться как bytea в БД
            if (hasFilter && ("type".equals(filterField) || "fuelType".equals(filterField))) {
                LOGGER.info("Фильтрация enum поля " + filterField + " в Java коде");
                
                // Загружаем все записи
                StringBuilder hql = new StringBuilder("FROM Vehicle v");
                
                // Добавление сортировки
                if (sortField != null && !sortField.isEmpty()) {
                    hql.append(" ORDER BY v.").append(sortField);
                    if (sortDirection != null && sortDirection.equalsIgnoreCase("desc")) {
                        hql.append(" DESC");
                    } else {
                        hql.append(" ASC");
                    }
                } else {
                    hql.append(" ORDER BY v.id ASC");
                }
                
                Query<Vehicle> query = session.createQuery(hql.toString(), Vehicle.class);
                List<Vehicle> allVehicles = query.getResultList();
                
                // Фильтруем в Java
                String lowerFilterValue = filterValue.toLowerCase();
                List<Vehicle> filtered = new java.util.ArrayList<>();
                
                for (Vehicle v : allVehicles) {
                    String fieldValue = null;
                    if ("type".equals(filterField) && v.getType() != null) {
                        fieldValue = v.getType().toString();
                    } else if ("fuelType".equals(filterField) && v.getFuelType() != null) {
                        fieldValue = v.getFuelType().toString();
                    }
                    
                    if (fieldValue != null && fieldValue.toLowerCase().contains(lowerFilterValue)) {
                        filtered.add(v);
                    }
                }
                
                LOGGER.info("Отфильтровано " + filtered.size() + " из " + allVehicles.size() + " записей");
                
                // Применяем пагинацию
                int start = page * size;
                int end = Math.min(start + size, filtered.size());
                
                if (start >= filtered.size()) {
                    return new java.util.ArrayList<>();
                }
                
                return filtered.subList(start, end);
            } else {
                // HQL запрос для обычных полей
                StringBuilder hql = new StringBuilder("FROM Vehicle v");
                
                if (hasFilter) {
                    hql.append(" WHERE LOWER(v.").append(filterField).append(") LIKE LOWER(:filterValue)");
                }

                // Добавление сортировки
                if (sortField != null && !sortField.isEmpty()) {
                    hql.append(" ORDER BY v.").append(sortField);
                    if (sortDirection != null && sortDirection.equalsIgnoreCase("desc")) {
                        hql.append(" DESC");
                    } else {
                        hql.append(" ASC");
                    }
                } else {
                    hql.append(" ORDER BY v.id ASC");
                }

                LOGGER.info("Executing HQL: " + hql.toString());
                
                Query<Vehicle> query = session.createQuery(hql.toString(), Vehicle.class);

                if (hasFilter) {
                    query.setParameter("filterValue", "%" + filterValue + "%");
                    LOGGER.info("Filter value: " + filterValue);
                }

                query.setFirstResult(page * size);
                query.setMaxResults(size);
                query.setCacheable(true);
                query.setCacheRegion("vehicle-filter-cache");

                List<Vehicle> results = query.getResultList();
                LOGGER.info("Found " + results.size() + " vehicles via HQL");
                
                return results;
            }
        } catch (Exception e) {
            LOGGER.severe("Ошибка при поиске Vehicle с фильтрами: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось найти Vehicle с фильтрами: " + e.getMessage(), e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public long countWithFilters(String filterField, String filterValue) {
        Session session = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();

            boolean hasFilter = filterField != null && !filterField.isEmpty() &&
                    filterValue != null && !filterValue.isEmpty();
            
            // Для enum полей (type, fuelType) используем подсчет в памяти
            if (hasFilter && ("type".equals(filterField) || "fuelType".equals(filterField))) {
                LOGGER.info("Подсчет для enum поля " + filterField + " в Java коде");
                
                // Загружаем все записи
                Query<Vehicle> query = session.createQuery("FROM Vehicle v", Vehicle.class);
                List<Vehicle> allVehicles = query.getResultList();
                
                // Считаем совпадения в Java
                String lowerFilterValue = filterValue.toLowerCase();
                long count = 0;
                
                for (Vehicle v : allVehicles) {
                    String fieldValue = null;
                    if ("type".equals(filterField) && v.getType() != null) {
                        fieldValue = v.getType().toString();
                    } else if ("fuelType".equals(filterField) && v.getFuelType() != null) {
                        fieldValue = v.getFuelType().toString();
                    }
                    
                    if (fieldValue != null && fieldValue.toLowerCase().contains(lowerFilterValue)) {
                        count++;
                    }
                }
                
                LOGGER.info("Count result: " + count);
                return count;
            } else {
                // HQL запрос для обычных полей
                StringBuilder hql = new StringBuilder("SELECT COUNT(v) FROM Vehicle v");
                
                if (hasFilter) {
                    hql.append(" WHERE LOWER(v.").append(filterField).append(") LIKE LOWER(:filterValue)");
                }

                LOGGER.info("Executing count HQL: " + hql.toString());
                
                Query<Long> query = session.createQuery(hql.toString(), Long.class);

                if (hasFilter) {
                    query.setParameter("filterValue", "%" + filterValue + "%");
                }

                // Включаем кэширование для запроса подсчета с фильтрами
                query.setCacheable(true);
                query.setCacheRegion("vehicle-count-cache");

                long count = query.getSingleResult();
                LOGGER.info("Count result via HQL: " + count);
                
                return count;
            }
        } catch (Exception e) {
            LOGGER.severe("Ошибка при подсчете Vehicle с фильтрами: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось подсчитать Vehicle с фильтрами: " + e.getMessage(), e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public boolean existsByNameAndType(String name, String type, Integer excludeId) {
        Session session = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            
            // Устанавливаем SERIALIZABLE для предотвращения phantom reads
            session.doWork(connection -> {
                connection.setTransactionIsolation(java.sql.Connection.TRANSACTION_SERIALIZABLE);
            });
            
            Query<Vehicle> query = session.createQuery(
                    "FROM Vehicle v WHERE v.name = :name", Vehicle.class);
            query.setParameter("name", name);
            
            List<Vehicle> results = query.getResultList();
            
            // Фильтруем по типу и excludeId в Java
            for (Vehicle v : results) {
                if (excludeId != null && excludeId.equals(v.getId())) {
                    continue;
                }
                
                String vehicleType = v.getType() != null ? v.getType().toString() : null;
                if ((type == null && vehicleType == null) || 
                    (type != null && type.equals(vehicleType))) {
                    return true;
                }
            }
            
            return false;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public boolean existsByTechnicalConfig(int enginePower, double capacity, String fuelType, Integer excludeId) {
        Session session = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            
            // Устанавливаем SERIALIZABLE для предотвращения phantom reads
            session.doWork(connection -> {
                connection.setTransactionIsolation(java.sql.Connection.TRANSACTION_SERIALIZABLE);
            });
            
            Query<Vehicle> query = session.createQuery(
                    "FROM Vehicle v WHERE v.enginePower = :enginePower AND v.capacity = :capacity", 
                    Vehicle.class);
            query.setParameter("enginePower", enginePower);
            query.setParameter("capacity", capacity);
            
            List<Vehicle> results = query.getResultList();
            
            // Фильтруем по fuelType и excludeId в Java
            for (Vehicle v : results) {
                if (excludeId != null && excludeId.equals(v.getId())) {
                    continue;
                }
                
                String vehicleFuelType = v.getFuelType() != null ? v.getFuelType().toString() : null;
                if ((fuelType == null && vehicleFuelType == null) || 
                    (fuelType != null && fuelType.equals(vehicleFuelType))) {
                    return true;
                }
            }
            
            return false;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
}



