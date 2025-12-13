package org.example.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.example.config.HibernateUtil;
import org.example.dto.VehicleImportDTO;
import org.example.entity.Vehicle;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;
import java.util.logging.Logger;

/**
 * Реализация сервиса для проверки ограничений уникальности на программном уровне
 * 
 * ВАЖНО: Эти ограничения реализованы ТОЛЬКО на программном уровне и НЕ отражены в БД
 */
@Stateless
public class UniquenessServiceImpl implements UniquenessService {

    private static final Logger LOGGER = Logger.getLogger(UniquenessServiceImpl.class.getName());

    @Inject
    private HibernateUtil hibernateUtil;

    @Override
    public void checkNameTypeUniqueness(String name, String type, Integer excludeId) {
        Session session = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            
            StringBuilder hql = new StringBuilder(
                "FROM Vehicle v WHERE v.name = :name");
            
            if (type != null) {
                hql.append(" AND v.type IS NOT NULL");
            } else {
                hql.append(" AND v.type IS NULL");
            }
            
            if (excludeId != null) {
                hql.append(" AND v.id != :excludeId");
            }
            
            Query<Vehicle> query = session.createQuery(hql.toString(), Vehicle.class);
            query.setParameter("name", name);
            
            if (excludeId != null) {
                query.setParameter("excludeId", excludeId);
            }
            
            List<Vehicle> results = query.getResultList();
            
            // Фильтруем по типу в Java (из-за проблем с сериализацией enum в БД)
            boolean hasDuplicate = results.stream().anyMatch(v -> {
                if (type == null) {
                    return v.getType() == null;
                }
                return v.getType() != null && v.getType().toString().equals(type);
            });
            
            if (hasDuplicate) {
                throw new IllegalArgumentException(
                    "Нарушение уникальности: Vehicle с именем '" + name + 
                    "' и типом '" + type + "' уже существует");
            }
            
            LOGGER.info("Проверка уникальности name+type пройдена: " + name + " / " + type);
            
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public void checkTechnicalConfigUniqueness(int enginePower, double capacity, String fuelType, Integer excludeId) {
        Session session = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            
            StringBuilder hql = new StringBuilder(
                "FROM Vehicle v WHERE v.enginePower = :enginePower AND v.capacity = :capacity");
            
            if (excludeId != null) {
                hql.append(" AND v.id != :excludeId");
            }
            
            Query<Vehicle> query = session.createQuery(hql.toString(), Vehicle.class);
            query.setParameter("enginePower", enginePower);
            query.setParameter("capacity", capacity);
            
            if (excludeId != null) {
                query.setParameter("excludeId", excludeId);
            }
            
            List<Vehicle> results = query.getResultList();
            
            // Фильтруем по fuelType в Java
            boolean hasDuplicate = results.stream().anyMatch(v -> {
                if (fuelType == null) {
                    return v.getFuelType() == null;
                }
                return v.getFuelType() != null && v.getFuelType().toString().equals(fuelType);
            });
            
            if (hasDuplicate) {
                throw new IllegalArgumentException(
                    "Нарушение уникальности: Vehicle с техническими характеристиками " +
                    "(enginePower=" + enginePower + ", capacity=" + capacity + 
                    ", fuelType=" + fuelType + ") уже существует");
            }
            
            LOGGER.info("Проверка уникальности технической конфигурации пройдена");
            
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public void validateUniqueness(VehicleImportDTO dto) {
        String type = dto.getType() != null ? dto.getType().toString() : null;
        String fuelType = dto.getFuelType() != null ? dto.getFuelType().toString() : null;
        
        checkNameTypeUniqueness(dto.getName(), type, null);
        checkTechnicalConfigUniqueness(dto.getEnginePower(), dto.getCapacity(), fuelType, null);
    }
}

