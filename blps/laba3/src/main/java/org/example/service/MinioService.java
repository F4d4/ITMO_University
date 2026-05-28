package org.example.service;

import io.minio.*;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket.uploaded}")
    private String uploadedBucket;

    @Value("${minio.bucket.published}")
    private String publishedBucket;

    /**
     * Загружает файл в бакет uploaded-videos.
     * @return имя объекта (key) в MinIO
     */
    public String uploadVideoFile(MultipartFile file, String objectName) {
        ensureBucketExists(uploadedBucket);
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(uploadedBucket)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType("video/mp4")
                            .build()
            );
            log.info("Файл '{}' загружен в бакет '{}'", objectName, uploadedBucket);
            return objectName;
        } catch (Exception e) {
            log.error("Ошибка загрузки файла в MinIO: {}", e.getMessage(), e);
            throw new BusinessException("Ошибка загрузки файла: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Копирует объект из бакета uploaded-videos в published-videos (публикация видео).
     * @return имя объекта в новом бакете
     */
    public String publishVideoFile(String objectName) {
        ensureBucketExists(publishedBucket);
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(publishedBucket)
                            .object(objectName)
                            .source(CopySource.builder()
                                    .bucket(uploadedBucket)
                                    .object(objectName)
                                    .build())
                            .build()
            );
            log.info("Файл '{}' скопирован из '{}' в '{}'", objectName, uploadedBucket, publishedBucket);
            return objectName;
        } catch (Exception e) {
            log.error("Ошибка публикации файла в MinIO: {}", e.getMessage(), e);
            throw new BusinessException("Ошибка публикации файла: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Удаляет объект из указанного бакета.
     */
    public void deleteObject(String bucketName, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            log.info("Объект '{}' удалён из бакета '{}'", objectName, bucketName);
        } catch (Exception e) {
            log.error("Ошибка удаления объекта из MinIO: {}", e.getMessage(), e);
        }
    }

    public String getUploadedBucket() {
        return uploadedBucket;
    }

    public String getPublishedBucket() {
        return publishedBucket;
    }

    private void ensureBucketExists(String bucketName) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );
                log.info("Создан бакет MinIO: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Ошибка проверки/создания бакета '{}': {}", bucketName, e.getMessage(), e);
            throw new BusinessException("Ошибка подключения к MinIO: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
