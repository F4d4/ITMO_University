package org.example.service;

import io.minio.*;
import io.minio.errors.*;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Сервис для работы с MinIO (S3-совместимое хранилище)
 * Управляет загрузкой и скачиванием файлов импорта
 */
@Singleton
public class MinioService {

    private static final Logger LOGGER = Logger.getLogger(MinioService.class.getName());

    // Конфигурация MinIO
    private static final String MINIO_URL = "http://127.0.0.1:8378";
    private static final String ACCESS_KEY = "minio";
    private static final String SECRET_KEY = "minio12345";
    private static final String BUCKET_NAME = "imports";

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        try {
            LOGGER.info("Инициализация MinIO клиента...");
            
            minioClient = MinioClient.builder()
                    .endpoint(MINIO_URL)
                    .credentials(ACCESS_KEY, SECRET_KEY)
                    .build();

            // Проверяем существование bucket
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(BUCKET_NAME)
                            .build()
            );

            if (!found) {
                LOGGER.warning("Bucket " + BUCKET_NAME + " не найден. Создаем новый bucket...");
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(BUCKET_NAME)
                                .build()
                );
                LOGGER.info("Bucket " + BUCKET_NAME + " успешно создан");
            } else {
                LOGGER.info("Bucket " + BUCKET_NAME + " найден");
            }

            LOGGER.info("MinIO клиент успешно инициализирован");

        } catch (Exception e) {
            LOGGER.severe("Ошибка при инициализации MinIO клиента: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось инициализировать MinIO", e);
        }
    }

    /**
     * Загрузить файл в MinIO
     * @param content содержимое файла (JSON строка)
     * @param originalFileName оригинальное имя файла (опционально)
     * @return имя файла в MinIO
     */
    public String uploadFile(String content, String originalFileName) throws Exception {
        String fileName = generateFileName(originalFileName);
        
        try {
            byte[] bytes = content.getBytes("UTF-8");
            ByteArrayInputStream stream = new ByteArrayInputStream(bytes);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(fileName)
                            .stream(stream, bytes.length, -1)
                            .contentType("application/json")
                            .build()
            );

            LOGGER.info("Файл успешно загружен в MinIO: " + fileName);
            return fileName;

        } catch (Exception e) {
            LOGGER.severe("Ошибка при загрузке файла в MinIO: " + e.getMessage());
            throw new Exception("Не удалось загрузить файл в MinIO: " + e.getMessage(), e);
        }
    }

    /**
     * Скачать файл из MinIO
     * @param fileName имя файла в MinIO
     * @return содержимое файла
     */
    public InputStream downloadFile(String fileName) throws Exception {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(fileName)
                            .build()
            );

        } catch (Exception e) {
            LOGGER.severe("Ошибка при скачивании файла из MinIO: " + e.getMessage());
            throw new Exception("Не удалось скачать файл из MinIO: " + e.getMessage(), e);
        }
    }

    /**
     * Удалить файл из MinIO
     * @param fileName имя файла в MinIO
     */
    public void deleteFile(String fileName) throws Exception {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(fileName)
                            .build()
            );

            LOGGER.info("Файл успешно удален из MinIO: " + fileName);

        } catch (Exception e) {
            LOGGER.severe("Ошибка при удалении файла из MinIO: " + e.getMessage());
            throw new Exception("Не удалось удалить файл из MinIO: " + e.getMessage(), e);
        }
    }

    /**
     * Проверить существование файла в MinIO
     * @param fileName имя файла
     * @return true если файл существует
     */
    public boolean fileExists(String fileName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(fileName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Генерировать уникальное имя файла
     */
    private String generateFileName(String originalFileName) {
        String uuid = UUID.randomUUID().toString();
        if (originalFileName != null && !originalFileName.isEmpty()) {
            String extension = "";
            int lastDot = originalFileName.lastIndexOf('.');
            if (lastDot > 0) {
                extension = originalFileName.substring(lastDot);
            }
            return uuid + extension;
        }
        return uuid + ".json";
    }

    /**
     * Получить MinIO клиент (для продвинутого использования)
     */
    public MinioClient getMinioClient() {
        return minioClient;
    }
}
