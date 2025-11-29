package org.example.service;

import org.example.dto.*;
import org.example.entity.VehicleType;

import java.util.List;

/**
 * Сервис для бизнес-логики работы с Vehicle
 */
public interface VehicleService {
    
    /**
     * Создать новый Vehicle
     */
    VehicleDTO createVehicle(VehicleCreateDTO createDTO);
    
    /**
     * Получить Vehicle по ID
     */
    VehicleDTO getVehicleById(Integer id);
    
    /**
     * Получить все Vehicle с пагинацией
     */
    PaginatedResponse<VehicleDTO> getAllVehicles(int page, int size);
    
    /**
     * Обновить существующий Vehicle
     */
    VehicleDTO updateVehicle(Integer id, VehicleUpdateDTO updateDTO);
    
    /**
     * Удалить Vehicle по ID
     */
    void deleteVehicle(Integer id);
    
    /**
     * Получить Vehicle с максимальным значением capacity
     */
    VehicleDTO getVehicleWithMaxCapacity();
    
    /**
     * Получить все Vehicle, у которых name начинается с заданной подстроки
     */
    List<VehicleDTO> getVehiclesByNamePrefix(String prefix);
    
    /**
     * Получить все Vehicle, у которых fuelConsumption больше заданного значения
     */
    List<VehicleDTO> getVehiclesByFuelConsumption(long minConsumption);
    
    /**
     * Получить все Vehicle заданного типа
     */
    List<VehicleDTO> getVehiclesByType(VehicleType type);
    
    /**
     * Скрутить счётчик пробега до нуля
     */
    void resetDistanceTravelled(Integer id);
    
    /**
     * Получить Vehicle с фильтрацией и сортировкой
     */
    PaginatedResponse<VehicleDTO> getVehiclesWithFilters(
        String filterField, String filterValue,
        String sortField, String sortDirection,
        int page, int size
    );
}









