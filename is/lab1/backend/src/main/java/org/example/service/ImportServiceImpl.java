package org.example.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.example.aop.ImportLock;
import org.example.aop.Logged;
import org.example.config.HibernateUtil;
import org.example.dto.ImportOperationDTO;
import org.example.dto.ImportResultDTO;
import org.example.dto.VehicleImportDTO;
import org.example.entity.*;
import org.example.repository.ImportOperationDAO;
import org.example.repository.UserDAO;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Реализация сервиса импорта с AOP и пессимистическими блокировками
 */
@Stateless
public class ImportServiceImpl implements ImportService {

    private static final Logger LOGGER = Logger.getLogger(ImportServiceImpl.class.getName());

    @Inject
    private HibernateUtil hibernateUtil;

    @Inject
    private UserDAO userDAO;

    @Inject
    private ImportOperationDAO importOperationDAO;

    @Inject
    private UniquenessService uniquenessService;

    @Override
    @Logged
    @ImportLock("vehicle-import")
    public ImportResultDTO importVehicles(List<VehicleImportDTO> vehicles, String username, boolean isAdmin) {
        LOGGER.info("Начало импорта " + vehicles.size() + " объектов пользователем " + username);

        // Получаем или создаем пользователя
        User user = userDAO.getOrCreateUser(username, isAdmin);

        // Создаем запись об операции импорта
        ImportOperation operation = new ImportOperation();
        operation.setUser(user);
        operation.setStatus(ImportStatus.IN_PROGRESS);
        operation = importOperationDAO.save(operation);

        Long operationId = operation.getId();

        Session session = null;
        Transaction tx = null;

        try {
            session = hibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            // Устанавливаем уровень изоляции SERIALIZABLE для предотвращения гонок
            session.doWork(connection -> {
                connection.setTransactionIsolation(java.sql.Connection.TRANSACTION_SERIALIZABLE);
            });

            int addedCount = 0;

            for (int i = 0; i < vehicles.size(); i++) {
                VehicleImportDTO dto = vehicles.get(i);

                // Валидация данных
                validateImportDTO(dto, i);

                // Проверка ограничений уникальности (на программном уровне)
                uniquenessService.validateUniqueness(dto);

                // Создание или получение координат
                Coordinates coordinates = getOrCreateCoordinates(session, dto.getX(), dto.getY());

                // Создание Vehicle
                Vehicle vehicle = new Vehicle();
                vehicle.setName(dto.getName());
                vehicle.setCoordinates(coordinates);
                vehicle.setCreationDate(new Date());
                vehicle.setType(dto.getType());
                vehicle.setEnginePower(dto.getEnginePower());
                vehicle.setNumberOfWheels(dto.getNumberOfWheels());
                vehicle.setCapacity(dto.getCapacity());
                vehicle.setDistanceTravelled(dto.getDistanceTravelled());
                vehicle.setFuelConsumption(dto.getFuelConsumption());
                vehicle.setFuelType(dto.getFuelType());

                session.persist(vehicle);
                addedCount++;

                LOGGER.info("Импортирован Vehicle #" + (i + 1) + ": " + dto.getName());
            }

            session.flush();
            tx.commit();

            // Обновляем статус операции
            operation.setStatus(ImportStatus.SUCCESS);
            operation.setAddedCount(addedCount);
            importOperationDAO.update(operation);

            LOGGER.info("Импорт успешно завершен. Добавлено объектов: " + addedCount);

            return new ImportResultDTO(operationId, "SUCCESS", addedCount, null);

        } catch (Exception e) {
            LOGGER.severe("Ошибка при импорте: " + e.getMessage());

            if (tx != null && tx.isActive()) {
                tx.rollback();
                LOGGER.info("Транзакция откачена - ни один объект не был сохранен");
            }

            // Обновляем статус операции
            operation.setStatus(ImportStatus.FAILED);
            operation.setErrorMessage(e.getMessage());
            operation.setAddedCount(0);
            importOperationDAO.update(operation);

            return new ImportResultDTO(operationId, "FAILED", 0, e.getMessage());

        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    @Logged
    public List<ImportOperationDTO> getImportHistory(String username, boolean isAdmin) {
        LOGGER.info("Запрос истории импорта. User: " + username + ", isAdmin: " + isAdmin);

        List<ImportOperation> operations;

        if (isAdmin) {
            // Администратор видит все операции
            operations = importOperationDAO.findAll();
        } else {
            // Обычный пользователь видит только свои операции
            User user = userDAO.getOrCreateUser(username, false);
            operations = importOperationDAO.findByUserId(user.getId());
        }

        return operations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Валидация данных импорта
     */
    private void validateImportDTO(VehicleImportDTO dto, int index) {
        String prefix = "Объект #" + (index + 1) + ": ";

        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException(prefix + "Имя не может быть пустым");
        }

        if (dto.getX() == null) {
            throw new IllegalArgumentException(prefix + "X координата обязательна");
        }

        if (dto.getY() == null) {
            throw new IllegalArgumentException(prefix + "Y координата обязательна");
        }

        if (dto.getY() > 621) {
            throw new IllegalArgumentException(prefix + "Y координата не может превышать 621");
        }

        if (dto.getEnginePower() <= 0) {
            throw new IllegalArgumentException(prefix + "Мощность двигателя должна быть больше 0");
        }

        if (dto.getNumberOfWheels() <= 0) {
            throw new IllegalArgumentException(prefix + "Количество колес должно быть больше 0");
        }

        if (dto.getCapacity() <= 0) {
            throw new IllegalArgumentException(prefix + "Вместимость должна быть больше 0");
        }

        if (dto.getDistanceTravelled() < 0) {
            throw new IllegalArgumentException(prefix + "Пройденное расстояние не может быть отрицательным");
        }

        if (dto.getFuelConsumption() <= 0) {
            throw new IllegalArgumentException(prefix + "Расход топлива должен быть больше 0");
        }
    }

    /**
     * Получить существующие координаты или создать новые
     */
    private Coordinates getOrCreateCoordinates(Session session, double x, long y) {
        // Ищем существующие координаты
        List<Coordinates> existing = session.createQuery(
                "FROM Coordinates c WHERE c.x = :x AND c.y = :y", Coordinates.class)
                .setParameter("x", x)
                .setParameter("y", y)
                .getResultList();

        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        // Создаем новые координаты
        Coordinates coords = new Coordinates(x, y);
        session.persist(coords);
        return coords;
    }

    /**
     * Конвертация в DTO
     */
    private ImportOperationDTO convertToDTO(ImportOperation operation) {
        ImportOperationDTO dto = new ImportOperationDTO();
        dto.setId(operation.getId());
        dto.setStatus(operation.getStatus().toString());
        dto.setUsername(operation.getUser().getUsername());
        dto.setAddedCount(operation.getAddedCount());
        dto.setErrorMessage(operation.getErrorMessage());
        dto.setCreatedAt(operation.getCreatedAt() != null ? operation.getCreatedAt().getTime() : null);
        return dto;
    }
}
