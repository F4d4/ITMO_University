package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.integration.bitrix.BitrixConnection;
import org.example.integration.bitrix.BitrixConnectionFactory;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BitrixIntegrationService {

    private final BitrixConnectionFactory bitrixConnectionFactory;

    public Long createPublicationReviewTask(Long videoId, String videoTitle, String description) {
        try (BitrixConnection connection = bitrixConnectionFactory.getConnection()) {
            String title = "Проверка публикации: " + videoTitle;
            String desc = "Видео ID: " + videoId + "\nОписание: " + description;
            Long taskId = connection.createTask(title, desc);
            log.info("Bitrix24: создана задача id={} на проверку публикации видео id={}", taskId, videoId);
            return taskId;
        } catch (Exception e) {
            log.error("Bitrix24: ошибка создания задачи для видео id={}: {}", videoId, e.getMessage());
            return null;
        }
    }

    public Integer getTaskStatus(Long bitrixTaskId) {
        try (BitrixConnection connection = bitrixConnectionFactory.getConnection()) {
            return connection.getTaskStatus(bitrixTaskId);
        } catch (Exception e) {
            log.error("Bitrix24: ошибка получения статуса задачи id={}: {}", bitrixTaskId, e.getMessage());
            return null;
        }
    }

    public Long createMonetizationReviewTask(Long methodId, String adName, String adType) {
        try (BitrixConnection connection = bitrixConnectionFactory.getConnection()) {
            String title = "Проверка рекламного метода: " + adName;
            String desc = "Method ID: " + methodId + "\nТип: " + adType + "\nНазвание: " + adName;
            Long taskId = connection.createTask(title, desc);
            log.info("Bitrix24: создана задача id={} на проверку метода монетизации id={}", taskId, methodId);
            return taskId;
        } catch (Exception e) {
            log.error("Bitrix24: ошибка создания задачи для метода id={}: {}", methodId, e.getMessage());
            return null;
        }
    }
}
