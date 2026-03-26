package org.example.service;

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
import org.springframework.transaction.annotation.Transactional;
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

    // ─── BPMN 1: Загрузка видео ──────────────────────────────────────────────────

    /**
     * Шаг 1 (BPMN 1): Клиент выбирает файл и нажимает «Загрузить видео».
     * Сервер валидирует файл (формат mp4, размер < 100 МБ).
     * При успехе сохраняет в MinIO (uploaded-videos) и создаёт запись Video со статусом UPLOADING.
     */
    @Transactional
    public VideoResponse uploadVideoFile(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", userId));

        // Валидация файла (BPMN 1: «Валидация файла»)
        validationService.validateVideoFile(file);

        String objectName = "video_" + UUID.randomUUID() + ".mp4";

        // Загрузка в MinIO
        minioService.uploadVideoFile(file, objectName);

        // Сохраняем запись видео со статусом UPLOADING
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
     * Шаг 2 (BPMN 1): Клиент заполняет информацию о видео (название, описание, теги).
     * Сервер валидирует описание (не более 150 символов).
     * При успехе обновляет запись Video и меняет статус на DRAFT.
     * Клиент получает уведомление о загрузке видео.
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

        // Валидация описания (BPMN 1: «Валидация описания видео»)
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
     * (BPMN 2): Клиент нажимает «Опубликовать видео».
     * 1. Выбирает тип аудитории (ALL_AGES / ADULTS_ONLY).
     * 2. Выбирает тип доступа (PUBLIC / PRIVATE).
     * Сервер проводит модерацию описания (проверка запрещённых слов).
     * При успехе копирует файл в бакет published-videos и меняет статус на PUBLISHED.
     * Клиент получает уведомление о публикации.
     */
    @Transactional
    public VideoResponse publishVideo(Long videoId, VideoPublishRequest request) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Видео", videoId));

        if (video.getStatus() != VideoStatus.DRAFT) {
            throw new BusinessException(
                    "Опубликовать можно только видео со статусом DRAFT. " +
                    "Текущий статус: " + video.getStatus()
            );
        }

        if (video.getTitle() == null || video.getTitle().isBlank()) {
            throw new BusinessException("Нельзя опубликовать видео без названия. " +
                    "Сначала заполните информацию о видео.");
        }

        // Модерация описания (BPMN 2: «Модерация видео»)
        validationService.moderateVideoDescription(video.getDescription());

        // Копируем файл в published-videos (BPMN 2: «Добавление видео в базу данных для просмотра»)
        minioService.publishVideoFile(video.getMinioKey());

        video.setAudienceType(request.getAudienceType());
        video.setAccessType(request.getAccessType());
        video.setBucketName(minioService.getPublishedBucket());
        video.setStatus(VideoStatus.PUBLISHED);

        video = videoRepository.save(video);
        log.info("Видео id={} опубликовано. Аудитория={}, Доступ={}", videoId,
                request.getAudienceType(), request.getAccessType());

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
