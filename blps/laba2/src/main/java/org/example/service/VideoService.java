package org.example.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.request.VideoInfoRequest;
import org.example.dto.request.VideoPublishRequest;
import org.example.dto.response.VideoResponse;
import org.example.entity.*;
import org.example.exception.BusinessException;
import org.example.exception.ResourceNotFoundException;
import org.example.repository.UserRepository;
import org.example.repository.VideoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final MinioService minioService;
    private final ValidationService validationService;
    private final PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @PostConstruct
    public void init() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // ─── BPMN 1: Загрузка видео ──────────────────────────────────────────────────

    /**
     * BPMN 1, Шаг 1: Клиент выбирает файл.
     * Сервер валидирует файл (формат mp4, размер < 100 МБ).
     * При успехе: сохраняет в MinIO и создаёт запись Video со статусом UPLOADING.
     */
    @Transactional
    public VideoResponse uploadVideoFile(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", userId));

        validationService.validateVideoFile(file);

        String objectName = "video_" + UUID.randomUUID() + ".mp4";
        minioService.uploadVideoFile(file, objectName);

        Video video = new Video();
        video.setUser(user);
        video.setMinioKey(objectName);
        video.setBucketName(minioService.getUploadedBucket());
        video.setStatus(VideoStatus.UPLOADING);

        video = videoRepository.save(video);
        log.info("Видео id={} загружено в MinIO, userId={}", video.getId(), userId);

        return toResponse(video);
    }

    /**
     * BPMN 1, Шаг 2: Клиент заполняет информацию о видео (название, описание, теги).
     * Сервер валидирует описание (не более 150 символов).
     * При успехе обновляет запись Video и меняет статус на DRAFT.
     */
    @Transactional
    public VideoResponse updateVideoInfo(Long videoId, VideoInfoRequest request) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Видео", videoId));

        if (video.getStatus() != VideoStatus.UPLOADING && video.getStatus() != VideoStatus.DRAFT) {
            throw new BusinessException(
                    "Нельзя редактировать информацию о видео со статусом: " + video.getStatus()
            );
        }

        validationService.validateDescription(request.getDescription());

        video.setTitle(request.getTitle());
        video.setDescription(request.getDescription());
        video.setTags(request.getTags());
        video.setStatus(VideoStatus.DRAFT);

        video = videoRepository.save(video);
        log.info("Информация о видео id={} обновлена, статус → DRAFT", videoId);

        return toResponse(video);
    }

    // ─── BPMN 2: Публикация видео ─────────────────────────────────────────────────

    /**
     * BPMN 2, Шаг 1 (USER): Клиент выбирает черновик и настраивает параметры публикации.
     * Сервер проводит автоматическую проверку описания.
     * При успехе: статус → PENDING_PUBLICATION (ждёт проверки модератора).
     */
    @Transactional
    public VideoResponse submitForPublication(Long videoId, VideoPublishRequest request) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Видео", videoId));

        if (video.getStatus() != VideoStatus.DRAFT) {
            throw new BusinessException(
                    "Отправить на публикацию можно только видео со статусом DRAFT. " +
                    "Текущий статус: " + video.getStatus()
            );
        }

        if (video.getTitle() == null || video.getTitle().isBlank()) {
            throw new BusinessException("Нельзя опубликовать видео без названия.");
        }

        // Автоматическая проверка описания (BPMN 2)
        validationService.moderateVideoDescription(video.getDescription());

        video.setAudienceType(request.getAudienceType());
        video.setAccessType(request.getAccessType());
        video.setStatus(VideoStatus.PENDING_PUBLICATION);

        video = videoRepository.save(video);
        log.info("Видео id={} отправлено на проверку модератором, статус → PENDING_PUBLICATION", videoId);

        return toResponse(video);
    }

    /**
     * BPMN 2, Шаг 2 (MODERATOR): Ручная проверка и одобрение публикации.
     * Транзакция (JTA/Narayana): изменить статус на PUBLISHED + сохранить параметры публикации
     * + скопировать файл в published-videos.
     */
    public VideoResponse approvePublication(Long videoId) {
        Video videoCheck = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Видео", videoId));

        if (videoCheck.getStatus() != VideoStatus.PENDING_PUBLICATION) {
            throw new BusinessException(
                    "Одобрить можно только видео со статусом PENDING_PUBLICATION. " +
                    "Текущий статус: " + videoCheck.getStatus()
            );
        }

        // Копируем файл в published-videos ДО транзакции (MinIO не является JTA-ресурсом)
        minioService.publishVideoFile(videoCheck.getMinioKey());

        // Программная JTA-транзакция: изменить статус + сохранить параметры публикации + добавить запись в БД
        VideoResponse result = transactionTemplate.execute(status -> {
            Video v = videoRepository.findById(videoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Видео", videoId));
            // Изменить статус видео на PUBLISHED
            v.setStatus(VideoStatus.PUBLISHED);
            // Сохранить параметры публикации (bucketName)
            v.setBucketName(minioService.getPublishedBucket());
            // Добавить запись в БД опубликованных видео
            Video saved = videoRepository.save(v);
            log.info("Видео id={} опубликовано модератором (JTA-транзакция)", videoId);
            // Маппинг внутри транзакции, чтобы lazy-relations были доступны
            return toResponse(saved);
        });

        return result;
    }

    /**
     * BPMN 2 (MODERATOR): Отклонение публикации.
     * Статус возвращается в DRAFT.
     */
    @Transactional
    public VideoResponse rejectPublication(Long videoId, String reason) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Видео", videoId));

        if (video.getStatus() != VideoStatus.PENDING_PUBLICATION) {
            throw new BusinessException(
                    "Отклонить можно только видео со статусом PENDING_PUBLICATION. " +
                    "Текущий статус: " + video.getStatus()
            );
        }

        video.setStatus(VideoStatus.DRAFT);
        video = videoRepository.save(video);
        log.info("Публикация видео id={} отклонена модератором. Причина: {}", videoId, reason);

        return toResponse(video);
    }

    // ─── Вспомогательные методы ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public VideoResponse getVideoById(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Видео", videoId));
        return toResponse(video);
    }

    @Transactional(readOnly = true)
    public List<VideoResponse> getVideosByUser(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", userId));
        return videoRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VideoResponse> getDraftsByUser(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", userId));
        return videoRepository.findByUserIdAndStatus(userId, VideoStatus.DRAFT).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VideoResponse> getAllPublishedVideos() {
        return videoRepository.findByStatus(VideoStatus.PUBLISHED).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VideoResponse> getPendingPublicationVideos() {
        return videoRepository.findByStatus(VideoStatus.PENDING_PUBLICATION).stream()
                .map(this::toResponse)
                .toList();
    }

    private VideoResponse toResponse(Video video) {
        VideoResponse response = new VideoResponse();
        response.setId(video.getId());
        response.setUserId(video.getUser().getId());
        response.setTitle(video.getTitle());
        response.setDescription(video.getDescription());
        response.setTags(video.getTags());
        response.setStatus(video.getStatus());
        response.setAudienceType(video.getAudienceType());
        response.setAccessType(video.getAccessType());
        response.setMinioKey(video.getMinioKey());
        response.setBucketName(video.getBucketName());
        response.setCreatedAt(video.getCreatedAt());
        response.setUpdatedAt(video.getUpdatedAt());
        return response;
    }
}
