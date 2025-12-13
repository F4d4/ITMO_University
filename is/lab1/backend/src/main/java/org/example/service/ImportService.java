package org.example.service;

import org.example.dto.ImportOperationDTO;
import org.example.dto.ImportResultDTO;
import org.example.dto.VehicleImportDTO;

import java.util.List;

/**
 * Сервис для импорта объектов
 */
public interface ImportService {
    
    /**
     * Импортировать список Vehicle из JSON
     * @param vehicles список DTO для импорта
     * @param username имя пользователя
     * @param isAdmin является ли пользователь администратором
     * @return результат операции импорта
     */
    ImportResultDTO importVehicles(List<VehicleImportDTO> vehicles, String username, boolean isAdmin);
    
    /**
     * Получить историю импорта
     * @param username имя пользователя
     * @param isAdmin является ли пользователь администратором (видит все операции)
     * @return список операций импорта
     */
    List<ImportOperationDTO> getImportHistory(String username, boolean isAdmin);
}

