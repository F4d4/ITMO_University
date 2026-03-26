package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.VideoInfoRequest;
import org.example.dto.request.VideoPublishRequest;
import org.example.dto.response.ApiResponse;
import org.example.dto.response.VideoResponse;
import org.example.service.VideoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    // ─── BPMN 1: Загрузка видео ──────────────────────────────────────────────────

    /**
     * BPMN 1, Шаг 1: Загрузка видеофайла.
     * Клиент выбирает файл и нажимает «Загрузить видео».
     * Сервер валидирует файл (mp4, < 100 МБ), сохраняет в MinIO.
     *
     * Параметры:
     * - userId: ID пользователя (автора)
     * - file: видеофайл (.mp4, до 100 МБ)
     *
     * Ответ: данные о созданной записи видео со статусом UPLOADING
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VideoResponse>> uploadVideo(
            @RequestParam("userId") Long userId,
            @RequestParam("file") MultipartFile file) {
        VideoResponse video = videoService.uploadVideoFile(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        "Файл успешно загружен. Заполните информацию о видео (title, description, tags).",
                        video
                ));
    }

    /**
     * BPMN 1, Шаг 2: Заполнение информации о видео.
     * Клиент вводит название, описание (до 150 символов) и теги.
     * Сервер валидирует описание и сохраняет данные.
     * Статус видео меняется с UPLOADING на DRAFT.
     * Клиент получает уведомление о загрузке.
     *
     * Ответ: обновлённые данные видео со статусом DRAFT
     */
    @PutMapping("/{videoId}/info")
    public ResponseEntity<ApiResponse<VideoResponse>> updateVideoInfo(
            @PathVariable Long videoId,
            @Valid @RequestBody VideoInfoRequest request) {
        VideoResponse video = videoService.updateVideoInfo(videoId, request);
        return ResponseEntity.ok(ApiResponse.ok(
                "Информация о видео сохранена. Видео добавлено в черновики.",
                video
        ));
    }

    // ─── BPMN 2: Публикация видео ─────────────────────────────────────────────────

    /**
     * BPMN 2: Публикация видео.
     * Клиент выбирает черновик, настраивает аудиторию и доступ.
     * Сервер проводит модерацию (проверяет описание на запрещённые слова).
     * При успехе: видео копируется в published-videos, статус → PUBLISHED.
     * Клиент получает уведомление о публикации.
     *
     * Параметры тела запроса:
     * - audienceType: ALL_AGES | ADULTS_ONLY
     * - accessType: PUBLIC | PRIVATE
     *
     * Ответ: данные опубликованного видео
     */
    @PostMapping("/{videoId}/publish")
    public ResponseEntity<ApiResponse<VideoResponse>> publishVideo(
            @PathVariable Long videoId,
            @Valid @RequestBody VideoPublishRequest request) {
        VideoResponse video = videoService.publishVideo(videoId, request);
        return ResponseEntity.ok(ApiResponse.ok(
                "Видео успешно опубликовано. Уведомление о публикации отправлено.",
                video
        ));
    }

    // ─── Вспомогательные эндпоинты ────────────────────────────────────────────────

    /**
     * Получить видео по ID
     */
    @GetMapping("/{videoId}")
    public ResponseEntity<ApiResponse<VideoResponse>> getVideoById(@PathVariable Long videoId) {
        VideoResponse video = videoService.getVideoById(videoId);
        return ResponseEntity.ok(ApiResponse.ok("Видео найдено", video));
    }

    /**
     * Получить все видео пользователя (все статусы)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<VideoResponse>>> getVideosByUser(
            @RequestParam("userId") Long userId) {
        List<VideoResponse> videos = videoService.getVideosByUser(userId);
        return ResponseEntity.ok(ApiResponse.ok("Список видео пользователя", videos));
    }

    /**
     * Получить черновики пользователя (BPMN 2: «Выбор видео из черновика»)
     */
    @GetMapping("/drafts")
    public ResponseEntity<ApiResponse<List<VideoResponse>>> getDraftsByUser(
            @RequestParam("userId") Long userId) {
        List<VideoResponse> drafts = videoService.getDraftsByUser(userId);
        return ResponseEntity.ok(ApiResponse.ok("Черновики пользователя", drafts));
    }

    /**
     * Получить все опубликованные видео
     */
    @GetMapping("/published")
    public ResponseEntity<ApiResponse<List<VideoResponse>>> getAllPublished() {
        List<VideoResponse> videos = videoService.getAllPublishedVideos();
        return ResponseEntity.ok(ApiResponse.ok("Опубликованные видео", videos));
    }
}
