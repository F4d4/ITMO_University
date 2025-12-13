package org.example.service;

import org.example.dto.VehicleImportDTO;

/**
 * Сервис для проверки ограничений уникальности на программном уровне
 * 
 * Ограничения уникальности (реализованы на программном уровне, не в БД):
 * 1. Уникальность комбинации (name + type) - нельзя создать два транспортных средства
 *    с одинаковым названием и типом
 * 2. Уникальность комбинации (enginePower + capacity + fuelType) - уникальная 
 *    техническая конфигурация транспортного средства
 */
public interface UniquenessService {
    
    /**
     * Проверить уникальность name + type
     * @param name название
     * @param type тип
     * @param excludeId ID для исключения (при обновлении)
     * @throws IllegalArgumentException если нарушено ограничение уникальности
     */
    void checkNameTypeUniqueness(String name, String type, Integer excludeId);
    
    /**
     * Проверить уникальность enginePower + capacity + fuelType
     * @param enginePower мощность двигателя
     * @param capacity вместимость
     * @param fuelType тип топлива
     * @param excludeId ID для исключения (при обновлении)
     * @throws IllegalArgumentException если нарушено ограничение уникальности
     */
    void checkTechnicalConfigUniqueness(int enginePower, double capacity, String fuelType, Integer excludeId);
    
    /**
     * Проверить все ограничения уникальности для импортируемого объекта
     * @param dto данные для импорта
     * @throws IllegalArgumentException если нарушено ограничение уникальности
     */
    void validateUniqueness(VehicleImportDTO dto);
}

