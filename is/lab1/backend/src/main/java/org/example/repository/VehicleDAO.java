package org.example.repository;

import jakarta.ejb.Local;
import org.example.entity.Vehicle;
import org.example.entity.VehicleType;

import java.util.List;
import java.util.Optional;

/**
 * DAO интерфейс для работы с сущностью Vehicle
 */
@Local
public interface VehicleDAO {
    
    /**
     * Сохранить новый объект Vehicle
     */
    Vehicle save(Vehicle vehicle);
    
    /**
     * Найти Vehicle по ID
     */
    Optional<Vehicle> findById(Integer id);
    
    /**
     * Найти Vehicle по ID с пессимистической блокировкой
     * Используется для предотвращения одновременного редактирования
     */
    Optional<Vehicle> findByIdWithLock(Integer id);
    
    /**
     * Получить все Vehicle с пагинацией
     */
    List<Vehicle> findAll(int page, int size);
    
    /**
     * Получить общее количество Vehicle
     */
    long count();
    
    /**
     * Обновить существующий Vehicle
     */
    Vehicle update(Vehicle vehicle);
    
    /**
     * Обновить Vehicle с пессимистической блокировкой в одной транзакции
     * @param id ID объекта
     * @param updater функция обновления полей
     * @return обновлённый Vehicle
     */
    Vehicle updateWithLock(Integer id, java.util.function.Consumer<Vehicle> updater);
    
    /**
     * Удалить Vehicle по ID
     */
    void deleteById(Integer id);
    
    /**
     * Удалить Vehicle с пессимистической блокировкой в одной транзакции
     */
    void deleteByIdWithLock(Integer id);
    
    /**
     * Проверить уникальность и сохранить Vehicle в одной транзакции
     * @param vehicle объект для сохранения
     * @param uniquenessChecker функция проверки уникальности
     * @return сохранённый Vehicle
     */
    Vehicle saveWithUniquenessCheck(Vehicle vehicle, java.util.function.Consumer<Vehicle> uniquenessChecker);
    
    /**
     * Найти Vehicle с максимальным значением capacity
     */
    Optional<Vehicle> findByMaxCapacity();
    
    /**
     * Найти все Vehicle, у которых name начинается с заданной подстроки
     */
    List<Vehicle> findByNameStartsWith(String prefix);
    
    /**
     * Найти все Vehicle, у которых fuelConsumption больше заданного значения
     */
    List<Vehicle> findByFuelConsumptionGreaterThan(long fuelConsumption);
    
    /**
     * Найти все Vehicle заданного типа
     */
    List<Vehicle> findByType(VehicleType type);
    
    /**
     * "Скрутить" счётчик пробега до нуля для Vehicle с заданным ID
     */
    void resetDistanceTravelled(Integer id);
    
    /**
     * Поиск с фильтрацией и сортировкой
     */
    List<Vehicle> findWithFilters(String filterField, String filterValue, 
                                   String sortField, String sortDirection,
                                   int page, int size);
    
    /**
     * Подсчет с фильтрацией
     */
    long countWithFilters(String filterField, String filterValue);
    
    /**
     * Проверить уникальность name + type
     */
    boolean existsByNameAndType(String name, String type, Integer excludeId);
    
    /**
     * Проверить уникальность enginePower + capacity + fuelType
     */
    boolean existsByTechnicalConfig(int enginePower, double capacity, String fuelType, Integer excludeId);
}














