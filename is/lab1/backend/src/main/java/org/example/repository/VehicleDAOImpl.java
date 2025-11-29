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
 * (не JTA, как в standalonetest проекте)
 */
@Stateless
public class VehicleDAOImpl implements VehicleDAO {

    private static final Logger LOGGER = Logger.getLogger(VehicleDAOImpl.class.getName());

    @Inject
    private HibernateUtil hibernateUtil;

    @Override
    public Vehicle save(Vehicle vehicle) {
        Transaction transaction = null;
        try (Session session = hibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(vehicle);
            transaction.commit();
            LOGGER.info("Vehicle успешно сохранен с ID: " + vehicle.getId());
            return vehicle;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            LOGGER.severe("Ошибка при сохранении Vehicle: " + e.getMessage());
            throw new RuntimeException("Не удалось сохранить Vehicle", e);
        }
    }

    @Override
    public Optional<Vehicle> findById(Integer id) {
        try (Session session = hibernateUtil.getSessionFactory().openSession()) {
            Vehicle vehicle = session.get(Vehicle.class, id);
            return Optional.ofNullable(vehicle);
        } catch (Exception e) {
            LOGGER.severe("Ошибка при поиске Vehicle по ID " + id + ": " + e.getMessage());
            throw new RuntimeException("Не удалось найти Vehicle по ID", e);
        }
    }

    @Override
    public List<Vehicle> findAll(int page, int size) {
        try (Session session = hibernateUtil.getSessionFactory().openSession()) {
            Query<Vehicle> query = session.createQuery(
                    "FROM Vehicle v ORDER BY v.id", Vehicle.class);
            query.setFirstResult(page * size);
            query.setMaxResults(size);
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при получении списка Vehicle: " + e.getMessage());
            throw new RuntimeException("Не удалось получить список Vehicle", e);
        }
    }

    @Override
    public long count() {
        try (Session session = hibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery(
                    "SELECT COUNT(v) FROM Vehicle v", Long.class);
            return query.getSingleResult();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при подсчете Vehicle: " + e.getMessage());
            throw new RuntimeException("Не удалось подсчитать количество Vehicle", e);
        }
    }

    @Override
    public Vehicle update(Vehicle vehicle) {
        Transaction transaction = null;
        try (Session session = hibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Vehicle merged = session.merge(vehicle);
            transaction.commit();
            LOGGER.info("Vehicle успешно обновлен с ID: " + vehicle.getId());
            return merged;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            LOGGER.severe("Ошибка при обновлении Vehicle: " + e.getMessage());
            throw new RuntimeException("Не удалось обновить Vehicle", e);
        }
    }

    @Override
    public void deleteById(Integer id) {
        Transaction transaction = null;
        try (Session session = hibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Vehicle vehicle = session.get(Vehicle.class, id);
            if (vehicle != null) {
                session.remove(vehicle);
                transaction.commit();
                LOGGER.info("Vehicle успешно удален с ID: " + id);
            } else {
                throw new IllegalArgumentException("Vehicle с ID " + id + " не найден");
            }
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            LOGGER.severe("Ошибка при удалении Vehicle: " + e.getMessage());
            throw new RuntimeException("Не удалось удалить Vehicle", e);
        }
    }

    @Override
    public Optional<Vehicle> findByMaxCapacity() {
        try (Session session = hibernateUtil.getSessionFactory().openSession()) {
            Query<Vehicle> query = session.createQuery(
                    "FROM Vehicle v WHERE v.capacity = (SELECT MAX(v2.capacity) FROM Vehicle v2)",
                    Vehicle.class);
            query.setMaxResults(1);
            List<Vehicle> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            LOGGER.severe("Ошибка при поиске Vehicle с максимальной capacity: " + e.getMessage());
            throw new RuntimeException("Не удалось найти Vehicle с максимальной capacity", e);
        }
    }

    @Override
    public List<Vehicle> findByNameStartsWith(String prefix) {
        try (Session session = hibernateUtil.getSessionFactory().openSession()) {
            Query<Vehicle> query = session.createQuery(
                    "FROM Vehicle v WHERE v.name LIKE :prefix",
                    Vehicle.class);
            query.setParameter("prefix", prefix + "%");
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при поиске Vehicle по префиксу имени: " + e.getMessage());
            throw new RuntimeException("Не удалось найти Vehicle по префиксу имени", e);
        }
    }

    @Override
    public List<Vehicle> findByFuelConsumptionGreaterThan(long fuelConsumption) {
        try (Session session = hibernateUtil.getSessionFactory().openSession()) {
            Query<Vehicle> query = session.createQuery(
                    "FROM Vehicle v WHERE v.fuelConsumption > :consumption",
                    Vehicle.class);
            query.setParameter("consumption", fuelConsumption);
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при поиске Vehicle по fuelConsumption: " + e.getMessage());
            throw new RuntimeException("Не удалось найти Vehicle по fuelConsumption", e);
        }
    }

    @Override
    public List<Vehicle> findByType(VehicleType type) {
        try (Session session = hibernateUtil.getSessionFactory().openSession()) {
            Query<Vehicle> query = session.createQuery(
                    "FROM Vehicle v WHERE v.type = :type",
                    Vehicle.class);
            query.setParameter("type", type);
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при поиске Vehicle по типу: " + e.getMessage());
            throw new RuntimeException("Не удалось найти Vehicle по типу", e);
        }
    }

    @Override
    public void resetDistanceTravelled(Integer id) {
        Transaction transaction = null;
        try (Session session = hibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Vehicle vehicle = session.get(Vehicle.class, id);
            if (vehicle != null) {
                vehicle.setDistanceTravelled(0);
                session.merge(vehicle);
                transaction.commit();
                LOGGER.info("Пробег успешно скручен для Vehicle с ID: " + id);
            } else {
                throw new IllegalArgumentException("Vehicle с ID " + id + " не найден");
            }
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            LOGGER.severe("Ошибка при сбросе пробега Vehicle: " + e.getMessage());
            throw new RuntimeException("Не удалось сбросить пробег Vehicle", e);
        }
    }

    @Override
    public List<Vehicle> findWithFilters(String filterField, String filterValue,
            String sortField, String sortDirection,
            int page, int size) {
        try (Session session = hibernateUtil.getSessionFactory().openSession()) {

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

                List<Vehicle> results = query.getResultList();
                LOGGER.info("Found " + results.size() + " vehicles via HQL");
                
                return results;
            }
        } catch (Exception e) {
            LOGGER.severe("Ошибка при поиске Vehicle с фильтрами: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось найти Vehicle с фильтрами: " + e.getMessage(), e);
        }
    }

    @Override
    public long countWithFilters(String filterField, String filterValue) {
        try (Session session = hibernateUtil.getSessionFactory().openSession()) {

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

                long count = query.getSingleResult();
                LOGGER.info("Count result via HQL: " + count);
                
                return count;
            }
        } catch (Exception e) {
            LOGGER.severe("Ошибка при подсчете Vehicle с фильтрами: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось подсчитать Vehicle с фильтрами: " + e.getMessage(), e);
        }
    }
}



