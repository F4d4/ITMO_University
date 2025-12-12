package org.example.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.example.dto.CoordinatesDTO;
import org.example.entity.Coordinates;
import org.example.repository.CoordinatesDAO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Реализация сервиса для работы с Coordinates
 */
@Stateless
public class CoordinatesServiceImpl implements CoordinatesService {
    
    @Inject
    private CoordinatesDAO coordinatesDAO;
    
    @Override
    public List<CoordinatesDTO> getAllCoordinates() {
        List<Coordinates> coordinates = coordinatesDAO.findAll();
        return coordinates.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public CoordinatesDTO getCoordinatesById(Long id) {
        Coordinates coordinates = coordinatesDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coordinates с ID " + id + " не найдены"));
        return convertToDTO(coordinates);
    }
    
    private CoordinatesDTO convertToDTO(Coordinates coordinates) {
        return new CoordinatesDTO(
                coordinates.getId(),
                coordinates.getX(),
                coordinates.getY()
        );
    }
}

