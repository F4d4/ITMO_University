package org.example.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.Video;
import org.example.entity.VideoStatus;
import org.example.exception.BusinessException;
import org.example.repository.VideoRepository;
import org.example.service.BitrixIntegrationService;
import org.example.service.ValidationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublicationTaskConsumer {

    private final VideoRepository videoRepository;
    private final BitrixIntegrationService bitrixIntegrationService;
    private final ValidationService validationService;
    private final ObjectMapper objectMapper;

    @Value("${app.node.id:node1}")
    private String nodeId;

    @JmsListener(destination = "publication-tasks")
    @Transactional
    public void processPublicationTask(String messageJson) {
        PublicationTaskMessage message;
        try {
            message = objectMapper.readValue(messageJson, PublicationTaskMessage.class);
        } catch (Exception e) {
            log.error("[{}] Ошибка парсинга сообщения из очереди: {}", nodeId, e.getMessage());
            throw new RuntimeException("Ошибка парсинга JMS сообщения", e);
        }

        log.info("[{}] Получено задание на обработку публикации видео id={}", nodeId, message.getVideoId());

        Video video = videoRepository.findById(message.getVideoId()).orElse(null);
        if (video == null || video.getStatus() != VideoStatus.PENDING_PUBLICATION) {
            log.warn("[{}] Видео id={} не найдено или статус изменился, пропуск",
                    nodeId, message.getVideoId());
            return;
        }

        try {
            validationService.moderateVideoDescription(video.getDescription());
        } catch (BusinessException e) {
            video.setStatus(VideoStatus.DRAFT);
            videoRepository.save(video);
            log.warn("[{}] Видео id={} автоматически отклонено при повторной проверке: {}",
                    nodeId, message.getVideoId(), e.getMessage());
            return;
        }

        Long bitrixTaskId = bitrixIntegrationService.createPublicationReviewTask(
                video.getId(), video.getTitle(), video.getDescription());

        if (bitrixTaskId != null) {
            video.setBitrixTaskId(bitrixTaskId);
            videoRepository.save(video);
        }

        log.info("[{}] Задание на публикацию видео id={} обработано, задача Bitrix24 id={}",
                nodeId, message.getVideoId(), bitrixTaskId);
    }
}
