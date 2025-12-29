package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.example.aop.CacheStatistics;
import org.example.aop.ImportLock;
import org.example.aop.Logged;
import org.example.config.HibernateUtil;
import org.example.dto.ImportOperationDTO;
import org.example.dto.ImportResultDTO;
import org.example.dto.VehicleImportDTO;
import org.example.entity.*;
import org.example.repository.ImportOperationDAO;
import org.example.repository.UserDAO;
import org.example.websocket.VehicleWebSocket;
import org.hibernate.Session;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Реализация сервиса импорта с распределенной транзакцией (2PC) и L2 кэшем
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
    private MinioService minioService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Logged
    @ImportLock("vehicle-import")
    @CacheStatistics(enabled = true) // Включаем логирование статистики кэша
    public ImportResultDTO importVehicles(List<VehicleImportDTO> vehicles, String username, boolean isAdmin) {
        LOGGER.info("=== Начало распределенной транзакции импорта ===");
        LOGGER.info("Количество объектов: " + vehicles.size() + ", Пользователь: " + username);

        // Получаем или создаем пользователя
        User user = userDAO.getOrCreateUser(username, isAdmin);

        // Создаем запись об операции импорта
        ImportOperation operation = new ImportOperation();
        operation.setUser(user);
        operation.setStatus(ImportStatus.IN_PROGRESS);
        operation = importOperationDAO.save(operation);

        Long operationId = operation.getId();

        // Создаем координатор распределенной транзакции
        DistributedTransactionCoordinator coordinator = new DistributedTransactionCoordinator(minioService);

        try {
            // Подготавливаем JSON файл для загрузки в MinIO
            String jsonContent = objectMapper.writeValueAsString(vehicles);
            
            LOGGER.info("[2PC] Фаза 1: PREPARE");
            
            // ФАЗА 1a: Подготовка MinIO
            coordinator.prepareMinIO(jsonContent, "import-" + operationId + ".json");
            
            // ФАЗА 1b: Подготовка БД (открываем транзакцию)
            Session session = hibernateUtil.getSessionFactory().openSession();
            coordinator.prepareDatabase(session);

            // Проверяем готовность обоих участников
            if (!coordinator.canCommit()) {
                throw new Exception("Участники транзакции не готовы к коммиту");
            }

            // Выполняем бизнес-логику в рамках подготовленной транзакции
            int addedCount = 0;
            Session dbSession = coordinator.getDbSession();

            for (int i = 0; i < vehicles.size(); i++) {
                VehicleImportDTO dto = vehicles.get(i);

                // Валидация данных
                validateImportDTO(dto, i);

                // Проверка ограничений уникальности
                String type = dto.getType() != null ? dto.getType().toString() : null;
                String fuelType = dto.getFuelType() != null ? dto.getFuelType().toString() : null;
                checkUniquenessInSession(dbSession, dto.getName(), type, 
                                         dto.getEnginePower(), dto.getCapacity(), fuelType, i);

                // Создание или получение координат
                Coordinates coordinates = getOrCreateCoordinates(dbSession, dto.getX(), dto.getY());

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

                dbSession.persist(vehicle);
                addedCount++;

                LOGGER.info("Импортирован Vehicle #" + (i + 1) + ": " + dto.getName());
            }

            LOGGER.info("[2PC] Фаза 2: COMMIT");
            
            // ФАЗА 2a: Коммит MinIO (загрузка файла) - ПЕРВЫМ, чтобы при ошибке откатилась БД
            coordinator.commitMinIO();
            LOGGER.info("[2PC] MinIO успешно зафиксирован");
            
            // ФАЗА 2b: Коммит БД - ВТОРЫМ, после успешной загрузки в MinIO
            coordinator.commitDatabase();
            LOGGER.info("[2PC] БД успешно зафиксирована");

            // Обновляем операцию импорта
            operation.setStatus(ImportStatus.SUCCESS);
            operation.setAddedCount(addedCount);
            operation.setFileName(coordinator.getMinioFileName());
            importOperationDAO.update(operation);

            LOGGER.info("=== Распределенная транзакция успешно завершена ===");
            LOGGER.info("Добавлено объектов: " + addedCount);
            LOGGER.info("Файл сохранен в MinIO: " + coordinator.getMinioFileName());

            // Уведомляем клиентов
            VehicleWebSocket.notifyImportCompleted(operationId, "SUCCESS", addedCount);

            return new ImportResultDTO(operationId, "SUCCESS", addedCount, null);

        } catch (Exception e) {
            LOGGER.severe("=== ОШИБКА в распределенной транзакции ===");
            LOGGER.severe("Причина: " + e.getMessage());
            e.printStackTrace();

            // ОТКАТ ТРАНЗАКЦИИ
            LOGGER.warning("[2PC] Выполняется полный откат транзакции");
            coordinator.rollbackAll();

            // Обновляем статус операции
            operation.setStatus(ImportStatus.FAILED);
            operation.setErrorMessage(e.getMessage());
            operation.setAddedCount(0);
            operation.setFileName(null);
            importOperationDAO.update(operation);

            // Уведомляем клиентов
            VehicleWebSocket.notifyImportCompleted(operationId, "FAILED", 0);

            return new ImportResultDTO(operationId, "FAILED", 0, e.getMessage());

        } finally {
            coordinator.close();
        }
    }

    @Override
    @Logged
    @CacheStatistics(enabled = true) // Включаем логирование статистики кэша
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
     * Проверка уникальности в рамках текущей сессии
     */
    private void checkUniquenessInSession(Session session, String name, String type,
                                          int enginePower, double capacity, String fuelType, int index) {
        String prefix = "Объект #" + (index + 1) + ": ";
        
        // Проверка уникальности name + type
        List<Vehicle> byName = session.createQuery(
                "FROM Vehicle v WHERE v.name = :name", Vehicle.class)
                .setParameter("name", name)
                .getResultList();
        
        for (Vehicle v : byName) {
            String vehicleType = v.getType() != null ? v.getType().toString() : null;
            if ((type == null && vehicleType == null) || 
                (type != null && type.equals(vehicleType))) {
                throw new IllegalArgumentException(prefix + 
                    "Нарушение уникальности: Vehicle с именем '" + name + 
                    "' и типом '" + type + "' уже существует");
            }
        }
        
        // Проверка уникальности технической конфигурации
        List<Vehicle> byConfig = session.createQuery(
                "FROM Vehicle v WHERE v.enginePower = :enginePower AND v.capacity = :capacity", 
                Vehicle.class)
                .setParameter("enginePower", enginePower)
                .setParameter("capacity", capacity)
                .getResultList();
        
        for (Vehicle v : byConfig) {
            String vehicleFuelType = v.getFuelType() != null ? v.getFuelType().toString() : null;
            if ((fuelType == null && vehicleFuelType == null) || 
                (fuelType != null && fuelType.equals(vehicleFuelType))) {
                throw new IllegalArgumentException(prefix + 
                    "Нарушение уникальности: Vehicle с техническими характеристиками " +
                    "(enginePower=" + enginePower + ", capacity=" + capacity + 
                    ", fuelType=" + fuelType + ") уже существует");
            }
        }
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
        dto.setFileName(operation.getFileName());
        dto.setCreatedAt(operation.getCreatedAt() != null ? operation.getCreatedAt().getTime() : null);
        return dto;
    }
}
