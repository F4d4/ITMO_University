package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class ValidationService {

    private static final long MAX_VIDEO_SIZE_BYTES = 100L * 1024 * 1024; // 100 МБ
    private static final int MAX_DESCRIPTION_LENGTH = 150;

    private static final List<String> FORBIDDEN_WORDS = Arrays.asList(
            "терроризм", "наркотики", "убийство", "ограбление", "шантаж", "насилие"
    );

    /**
     * BPMN 1: Валидация видеофайла.
     * Проверяет: формат mp4, размер < 100 МБ
     */
    public void validateVideoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Файл не выбран или пустой");
        }

        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();

        boolean isMp4ByExtension = originalFilename != null &&
                originalFilename.toLowerCase().endsWith(".mp4");
        boolean isMp4ByContentType = "video/mp4".equalsIgnoreCase(contentType);

        if (!isMp4ByExtension && !isMp4ByContentType) {
            log.warn("Попытка загрузить файл недопустимого формата: contentType={}, filename={}",
                    contentType, originalFilename);
            throw new BusinessException(
                    "Недопустимый формат файла. Допускается только MP4. " +
                    "Текущий формат: " + (contentType != null ? contentType : "неизвестен"),
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }

        if (file.getSize() >= MAX_VIDEO_SIZE_BYTES) {
            log.warn("Попытка загрузить файл слишком большого размера: {} байт", file.getSize());
            throw new BusinessException(
                    "Размер файла превышает допустимый предел. Максимум: 100 МБ, " +
                    "текущий размер: " + (file.getSize() / (1024 * 1024)) + " МБ",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }

        log.info("Валидация файла пройдена: {}, размер: {} байт", originalFilename, file.getSize());
    }

    /**
     * BPMN 1: Валидация описания видео.
     * Проверяет: длина не более 150 символов
     */
    public void validateDescription(String description) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessException(
                    "Описание превышает максимально допустимую длину. " +
                    "Максимум: " + MAX_DESCRIPTION_LENGTH + " символов, " +
                    "текущая длина: " + description.length() + " символов"
            );
        }
    }

    /**
     * BPMN 2: Модерация видео.
     * Проверяет описание на наличие запрещённых слов.
     */
    public void moderateVideoDescription(String description) {
        if (description == null || description.isBlank()) {
            return;
        }
        checkForbiddenWords(description, "Описание видео");
    }

    /**
     * BPMN 4: Модерация рекламного объявления.
     * Проверяет название рекламы на наличие запрещённых слов.
     */
    public void moderateAdName(String adName) {
        if (adName == null || adName.isBlank()) {
            throw new BusinessException("Название рекламного объявления не может быть пустым");
        }
        checkForbiddenWords(adName, "Название рекламы");
    }

    private void checkForbiddenWords(String text, String fieldName) {
        String lowerText = text.toLowerCase();
        for (String word : FORBIDDEN_WORDS) {
            if (lowerText.contains(word.toLowerCase())) {
                log.warn("Обнаружено запрещённое слово '{}' в поле '{}'", word, fieldName);
                throw new BusinessException(
                        fieldName + " содержит запрещённое слово: \"" + word + "\". " +
                        "Публикация отклонена модерацией.",
                        HttpStatus.UNPROCESSABLE_ENTITY
                );
            }
        }
    }
}
