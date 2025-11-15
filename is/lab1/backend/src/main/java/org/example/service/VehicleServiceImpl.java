package org.example.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.example.dto.*;
import org.example.entity.Coordinates;
import org.example.entity.Vehicle;
import org.example.entity.VehicleType;
import org.example.repository.VehicleDAO;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Реализация сервиса для работы с Vehicle
 */
@Stateless
public class VehicleServiceImpl implements VehicleService {
    
    private static final Logger LOGGER = Logger.getLogger(VehicleServiceImpl.class.getName());
    
    @Inject
    private VehicleDAO vehicleDAO;
    
    @Override
    public VehicleDTO createVehicle(VehicleCreateDTO createDTO) {
        try {
            validateCreateDTO(createDTO);
            
            Vehicle vehicle = new Vehicle();
            vehicle.setName(createDTO.getName());
            
            // Создание координат
            Coordinates coordinates = new Coordinates(createDTO.getX(), createDTO.getY());
            vehicle.setCoordinates(coordinates);
            
            vehicle.setCreationDate(new Date());
            vehicle.setType(createDTO.getType());
            vehicle.setEnginePower(createDTO.getEnginePower());
            vehicle.setNumberOfWheels(createDTO.getNumberOfWheels());
            vehicle.setCapacity(createDTO.getCapacity());
            vehicle.setDistanceTravelled(createDTO.getDistanceTravelled());
            vehicle.setFuelConsumption(createDTO.getFuelConsumption());
            vehicle.setFuelType(createDTO.getFuelType());
            
            Vehicle saved = vehicleDAO.save(vehicle);
            LOGGER.info("Vehicle успешно создан с ID: " + saved.getId());
            
            return convertToDTO(saved);
        } catch (Exception e) {
            LOGGER.severe("Ошибка при создании Vehicle: " + e.getMessage());
            throw new RuntimeException("Не удалось создать Vehicle", e);
        }
    }
    
    @Override
    public VehicleDTO getVehicleById(Integer id) {
        Vehicle vehicle = vehicleDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Vehicle с ID " + id + " не найден"));
        return convertToDTO(vehicle);
    }
    
    @Override
    public PaginatedResponse<VehicleDTO> getAllVehicles(int page, int size) {
        List<Vehicle> vehicles = vehicleDAO.findAll(page, size);
        long total = vehicleDAO.count();
        
        List<VehicleDTO> dtos = vehicles.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return new PaginatedResponse<>(dtos, total, page, size);
    }
    
    @Override
    public VehicleDTO updateVehicle(Integer id, VehicleUpdateDTO updateDTO) {
        try {
            Vehicle vehicle = vehicleDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle с ID " + id + " не найден"));
            
            validateUpdateDTO(updateDTO);
            
            // Обновление полей
            if (updateDTO.getName() != null) {
                vehicle.setName(updateDTO.getName());
            }
            
            if (updateDTO.getX() != null || updateDTO.getY() != null) {
                Coordinates coords = vehicle.getCoordinates();
                if (updateDTO.getX() != null) {
                    coords.setX(updateDTO.getX());
                }
                if (updateDTO.getY() != null) {
                    coords.setY(updateDTO.getY());
                }
            }
            
            if (updateDTO.getType() != null) {
                vehicle.setType(updateDTO.getType());
            }
            
            if (updateDTO.getEnginePower() != null) {
                vehicle.setEnginePower(updateDTO.getEnginePower());
            }
            
            if (updateDTO.getNumberOfWheels() != null) {
                vehicle.setNumberOfWheels(updateDTO.getNumberOfWheels());
            }
            
            if (updateDTO.getCapacity() != null) {
                vehicle.setCapacity(updateDTO.getCapacity());
            }
            
            if (updateDTO.getDistanceTravelled() != null) {
                vehicle.setDistanceTravelled(updateDTO.getDistanceTravelled());
            }
            
            if (updateDTO.getFuelConsumption() != null) {
                vehicle.setFuelConsumption(updateDTO.getFuelConsumption());
            }
            
            if (updateDTO.getFuelType() != null) {
                vehicle.setFuelType(updateDTO.getFuelType());
            }
            
            Vehicle updated = vehicleDAO.update(vehicle);
            LOGGER.info("Vehicle успешно обновлен с ID: " + id);
            
            return convertToDTO(updated);
        } catch (Exception e) {
            LOGGER.severe("Ошибка при обновлении Vehicle: " + e.getMessage());
            throw new RuntimeException("Не удалось обновить Vehicle", e);
        }
    }
    
    @Override
    public void deleteVehicle(Integer id) {
        try {
            // Проверяем существование Vehicle перед удалением
            vehicleDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle с ID " + id + " не найден"));
            
            vehicleDAO.deleteById(id);
            LOGGER.info("Vehicle успешно удален с ID: " + id);
        } catch (Exception e) {
            LOGGER.severe("Ошибка при удалении Vehicle: " + e.getMessage());
            throw new RuntimeException("Не удалось удалить Vehicle", e);
        }
    }
    
    @Override
    public VehicleDTO getVehicleWithMaxCapacity() {
        Vehicle vehicle = vehicleDAO.findByMaxCapacity()
            .orElseThrow(() -> new IllegalArgumentException("Не найдено ни одного Vehicle"));
        return convertToDTO(vehicle);
    }
    
    @Override
    public List<VehicleDTO> getVehiclesByNamePrefix(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            throw new IllegalArgumentException("Префикс не может быть пустым");
        }
        
        List<Vehicle> vehicles = vehicleDAO.findByNameStartsWith(prefix);
        return vehicles.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<VehicleDTO> getVehiclesByFuelConsumption(long minConsumption) {
        if (minConsumption < 0) {
            throw new IllegalArgumentException("Минимальное потребление топлива не может быть отрицательным");
        }
        
        List<Vehicle> vehicles = vehicleDAO.findByFuelConsumptionGreaterThan(minConsumption);
        return vehicles.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<VehicleDTO> getVehiclesByType(VehicleType type) {
        if (type == null) {
            throw new IllegalArgumentException("Тип Vehicle не может быть null");
        }
        
        List<Vehicle> vehicles = vehicleDAO.findByType(type);
        return vehicles.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    public void resetDistanceTravelled(Integer id) {
        vehicleDAO.resetDistanceTravelled(id);
        LOGGER.info("Пробег успешно сброшен для Vehicle с ID: " + id);
    }
    
    @Override
    public PaginatedResponse<VehicleDTO> getVehiclesWithFilters(
            String filterField, String filterValue,
            String sortField, String sortDirection,
            int page, int size) {
        
        List<Vehicle> vehicles = vehicleDAO.findWithFilters(
            filterField, filterValue, sortField, sortDirection, page, size
        );
        
        long total = vehicleDAO.countWithFilters(filterField, filterValue);
        
        List<VehicleDTO> dtos = vehicles.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return new PaginatedResponse<>(dtos, total, page, size);
    }
    
    // Вспомогательные методы
    
    private VehicleDTO convertToDTO(Vehicle vehicle) {
        VehicleDTO dto = new VehicleDTO();
        dto.setId(vehicle.getId());
        dto.setName(vehicle.getName());
        dto.setX(vehicle.getCoordinates().getX());
        dto.setY(vehicle.getCoordinates().getY());
        dto.setCreationDate(vehicle.getCreationDate());
        dto.setType(vehicle.getType());
        dto.setEnginePower(vehicle.getEnginePower());
        dto.setNumberOfWheels(vehicle.getNumberOfWheels());
        dto.setCapacity(vehicle.getCapacity());
        dto.setDistanceTravelled(vehicle.getDistanceTravelled());
        dto.setFuelConsumption(vehicle.getFuelConsumption());
        dto.setFuelType(vehicle.getFuelType());
        return dto;
    }
    
    private void validateCreateDTO(VehicleCreateDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Имя не может быть пустым");
        }
        if (dto.getY() > 621) {
            throw new IllegalArgumentException("Y координата не может превышать 621");
        }
        if (dto.getEnginePower() <= 0) {
            throw new IllegalArgumentException("Мощность двигателя должна быть больше 0");
        }
        if (dto.getNumberOfWheels() <= 0) {
            throw new IllegalArgumentException("Количество колес должно быть больше 0");
        }
        if (dto.getCapacity() <= 0) {
            throw new IllegalArgumentException("Вместимость должна быть больше 0");
        }
        if (dto.getDistanceTravelled() < 0) {
            throw new IllegalArgumentException("Пройденное расстояние не может быть отрицательным");
        }
        if (dto.getFuelConsumption() <= 0) {
            throw new IllegalArgumentException("Расход топлива должен быть больше 0");
        }
    }
    
    private void validateUpdateDTO(VehicleUpdateDTO dto) {
        if (dto.getName() != null && dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Имя не может быть пустым");
        }
        if (dto.getY() != null && dto.getY() > 621) {
            throw new IllegalArgumentException("Y координата не может превышать 621");
        }
        if (dto.getEnginePower() != null && dto.getEnginePower() <= 0) {
            throw new IllegalArgumentException("Мощность двигателя должна быть больше 0");
        }
        if (dto.getNumberOfWheels() != null && dto.getNumberOfWheels() <= 0) {
            throw new IllegalArgumentException("Количество колес должно быть больше 0");
        }
        if (dto.getCapacity() != null && dto.getCapacity() <= 0) {
            throw new IllegalArgumentException("Вместимость должна быть больше 0");
        }
        if (dto.getDistanceTravelled() != null && dto.getDistanceTravelled() < 0) {
            throw new IllegalArgumentException("Пройденное расстояние не может быть отрицательным");
        }
        if (dto.getFuelConsumption() != null && dto.getFuelConsumption() <= 0) {
            throw new IllegalArgumentException("Расход топлива должен быть больше 0");
        }
    }
}

