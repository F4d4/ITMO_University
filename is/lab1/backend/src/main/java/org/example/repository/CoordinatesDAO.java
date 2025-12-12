package org.example.repository;

import jakarta.ejb.Local;
import org.example.entity.Coordinates;

import java.util.List;
import java.util.Optional;

/**
 * DAO интерфейс для работы с Coordinates
 */
@Local
public interface CoordinatesDAO {
    
    /**
     * Сохранить координаты
     */
    Coordinates save(Coordinates coordinates);
    
    /**
     * Найти координаты по ID
     */
    Optional<Coordinates> findById(Long id);
    
    /**
     * Найти координаты по x и y
     */
    Optional<Coordinates> findByXAndY(double x, long y);
    
    /**
     * Получить все координаты
     */
    List<Coordinates> findAll();
    
    /**
     * Подсчитать количество координат
     */
    long count();
}






