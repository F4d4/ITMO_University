package org.example.service;

import jakarta.inject.Inject;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Координатор распределенных транзакций
 * Реализует двухфазный коммит (2PC) для транзакций между БД и MinIO
 * 
 * Фазы:
 * 1. Prepare (подготовка) - проверка возможности выполнения операций
 * 2. Commit (фиксация) - применение изменений
 * 
 * В случае ошибки на любой фазе выполняется Rollback (откат)
 */
public class DistributedTransactionCoordinator {

    private static final Logger LOGGER = Logger.getLogger(DistributedTransactionCoordinator.class.getName());

    private MinioService minioService;

    private String transactionId;
    private Session dbSession;
    private Transaction dbTransaction;
    private String minioFileName;
    private String fileContent;
    private boolean dbPrepared = false;
    private boolean minioPrepared = false;
    private boolean dbCommitted = false;
    private boolean minioCommitted = false;

    public DistributedTransactionCoordinator(MinioService minioService) {
        this.transactionId = UUID.randomUUID().toString();
        this.minioService = minioService;
    }

    /**
     * Фаза 1: Подготовка базы данных
     */
    public void prepareDatabase(Session session) throws Exception {
        LOGGER.info("[TX:" + transactionId + "] PREPARE: База данных");
        
        try {
            this.dbSession = session;
            this.dbTransaction = session.beginTransaction();
            
            // Устанавливаем уровень изоляции SERIALIZABLE
            session.doWork(connection -> {
                connection.setTransactionIsolation(java.sql.Connection.TRANSACTION_SERIALIZABLE);
            });
            
            dbPrepared = true;
            LOGGER.info("[TX:" + transactionId + "] PREPARE БД: успешно");
            
        } catch (Exception e) {
            dbPrepared = false;
            LOGGER.severe("[TX:" + transactionId + "] PREPARE БД: ошибка - " + e.getMessage());
            throw new Exception("Ошибка подготовки БД: " + e.getMessage(), e);
        }
    }

    /**
     * Фаза 1: Подготовка MinIO (валидация файла)
     */
    public void prepareMinIO(String content, String originalFileName) throws Exception {
        LOGGER.info("[TX:" + transactionId + "] PREPARE: MinIO");
        
        try {
            this.fileContent = content;
            
            // Валидация: проверяем что файл не пустой
            if (content == null || content.trim().isEmpty()) {
                throw new Exception("Файл пустой");
            }
            
            // Генерируем имя файла (но пока не загружаем)
            this.minioFileName = UUID.randomUUID().toString() + 
                    (originalFileName != null && originalFileName.endsWith(".json") ? ".json" : ".json");
            
            minioPrepared = true;
            LOGGER.info("[TX:" + transactionId + "] PREPARE MinIO: успешно (файл: " + minioFileName + ")");
            
        } catch (Exception e) {
            minioPrepared = false;
            LOGGER.severe("[TX:" + transactionId + "] PREPARE MinIO: ошибка - " + e.getMessage());
            throw new Exception("Ошибка подготовки MinIO: " + e.getMessage(), e);
        }
    }

    /**
     * Проверка готовности всех участников
     */
    public boolean canCommit() {
        boolean ready = dbPrepared && minioPrepared;
        LOGGER.info("[TX:" + transactionId + "] Готовность к коммиту: " + 
                   (ready ? "ДА" : "НЕТ") + 
                   " (БД:" + dbPrepared + ", MinIO:" + minioPrepared + ")");
        return ready;
    }

    /**
     * Фаза 2: Коммит базы данных
     */
    public void commitDatabase() throws Exception {
        LOGGER.info("[TX:" + transactionId + "] COMMIT: База данных");
        
        if (!dbPrepared) {
            throw new Exception("БД не подготовлена для коммита");
        }
        
        try {
            if (dbTransaction != null && dbTransaction.isActive()) {
                dbTransaction.commit();
                dbCommitted = true;
                LOGGER.info("[TX:" + transactionId + "] COMMIT БД: успешно");
            }
        } catch (Exception e) {
            dbCommitted = false;
            LOGGER.severe("[TX:" + transactionId + "] COMMIT БД: ошибка - " + e.getMessage());
            throw new Exception("Ошибка коммита БД: " + e.getMessage(), e);
        }
    }

    /**
     * Фаза 2: Коммит MinIO (загрузка файла)
     */
    public void commitMinIO() throws Exception {
        LOGGER.info("[TX:" + transactionId + "] COMMIT: MinIO");
        
        if (!minioPrepared) {
            throw new Exception("MinIO не подготовлен для коммита");
        }
        
        try {
            // Теперь действительно загружаем файл
            String uploadedFileName = minioService.uploadFile(fileContent, minioFileName);
            this.minioFileName = uploadedFileName;
            minioCommitted = true;
            LOGGER.info("[TX:" + transactionId + "] COMMIT MinIO: успешно");
            
        } catch (Exception e) {
            minioCommitted = false;
            LOGGER.severe("[TX:" + transactionId + "] COMMIT MinIO: ошибка - " + e.getMessage());
            throw new Exception("Ошибка коммита MinIO: " + e.getMessage(), e);
        }
    }

    /**
     * Откат транзакции базы данных
     */
    public void rollbackDatabase() {
        LOGGER.warning("[TX:" + transactionId + "] ROLLBACK: База данных");
        
        try {
            if (dbTransaction != null && dbTransaction.isActive()) {
                dbTransaction.rollback();
                LOGGER.info("[TX:" + transactionId + "] ROLLBACK БД: выполнен");
            }
        } catch (Exception e) {
            LOGGER.severe("[TX:" + transactionId + "] ROLLBACK БД: ошибка - " + e.getMessage());
        } finally {
            dbCommitted = false;
        }
    }

    /**
     * Откат транзакции MinIO (удаление файла)
     */
    public void rollbackMinIO() {
        LOGGER.warning("[TX:" + transactionId + "] ROLLBACK: MinIO");
        
        if (minioCommitted && minioFileName != null) {
            try {
                minioService.deleteFile(minioFileName);
                LOGGER.info("[TX:" + transactionId + "] ROLLBACK MinIO: файл удален");
            } catch (Exception e) {
                LOGGER.severe("[TX:" + transactionId + "] ROLLBACK MinIO: ошибка удаления файла - " + e.getMessage());
            }
        }
        minioCommitted = false;
    }

    /**
     * Полный откат всей распределенной транзакции
     */
    public void rollbackAll() {
        LOGGER.warning("[TX:" + transactionId + "] ROLLBACK: Полный откат распределенной транзакции");
        rollbackDatabase();
        rollbackMinIO();
    }

    /**
     * Закрыть ресурсы
     */
    public void close() {
        if (dbSession != null && dbSession.isOpen()) {
            dbSession.close();
        }
    }

    // Геттеры
    public String getTransactionId() {
        return transactionId;
    }

    public String getMinioFileName() {
        return minioFileName;
    }

    public boolean isDbCommitted() {
        return dbCommitted;
    }

    public boolean isMinioCommitted() {
        return minioCommitted;
    }

    public Session getDbSession() {
        return dbSession;
    }
}
