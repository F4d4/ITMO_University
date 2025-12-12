package org.example.service;

import org.example.dto.CoordinatesDTO;

import java.util.List;

/**
 * Сервис для работы с Coordinates
 */
public interface CoordinatesService {
    
    /**
     * Получить все координаты
     */
    List<CoordinatesDTO> getAllCoordinates();
    
    /**
     * Получить координаты по ID
     */
    CoordinatesDTO getCoordinatesById(Long id);
}





