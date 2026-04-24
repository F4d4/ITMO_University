package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.VideoInfoRequest;
import org.example.dto.request.VideoPublishRequest;
import org.example.dto.response.ApiResponse;
import org.example.dto.response.VideoResponse;
import org.example.security.SecurityUtils;
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
    private final SecurityUtils securityUtils;

    // ─── BPMN 1: Загрузка видео ──────────────────────────────────────────────────

    /**
     * BPMN 1, Шаг 1: Загрузка видеофайла (USER).
     * userId берётся из JWT токена.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VideoResponse>> uploadVideo(
            @RequestParam("file") MultipartFile file) {
        Long userId = securityUtils.getCurrentUserId();
        VideoResponse video = videoService.uploadVideoFile(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        "Файл успешно загружен. Заполните информацию о видео (title, description, tags).",
                        video
                ));
    }

    /**
     * BPMN 1, Шаг 2: Заполнение информации о видео (USER).
     * Сервер проверяет, что видео принадлежит текущему пользователю.
     */
    @PutMapping("/{videoId}/info")
    public ResponseEntity<ApiResponse<VideoResponse>> updateVideoInfo(
            @PathVariable Long videoId,
            @Valid @RequestBody VideoInfoRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        VideoResponse video = videoService.updateVideoInfo(videoId, userId, request);
        return ResponseEntity.ok(ApiResponse.ok(
                "Информация о видео сохранена. Видео добавлено в черновики.",
                video
        ));
    }

    // ─── BPMN 2: Публикация видео ─────────────────────────────────────────────────

    /**
     * BPMN 2, Шаг 1 (USER): Отправка видео на публикацию.
     * Сервер проверяет, что видео принадлежит текущему пользователю.
     */
    @PostMapping("/{videoId}/submit-publish")
    public ResponseEntity<ApiResponse<VideoResponse>> submitForPublication(
            @PathVariable Long videoId,
            @Valid @RequestBody VideoPublishRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        VideoResponse video = videoService.submitForPublication(videoId, userId, request);
        return ResponseEntity.ok(ApiResponse.ok(
                "Видео отправлено на проверку модератором. Статус: PENDING_PUBLICATION.",
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
     * Получить все видео текущего пользователя (userId из JWT)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<VideoResponse>>> getMyVideos() {
        Long userId = securityUtils.getCurrentUserId();
        List<VideoResponse> videos = videoService.getVideosByUser(userId);
        return ResponseEntity.ok(ApiResponse.ok("Список видео пользователя", videos));
    }

    /**
     * BPMN 2: Получить черновики текущего пользователя (userId из JWT)
     */
    @GetMapping("/drafts")
    public ResponseEntity<ApiResponse<List<VideoResponse>>> getMyDrafts() {
        Long userId = securityUtils.getCurrentUserId();
        List<VideoResponse> drafts = videoService.getDraftsByUser(userId);
        return ResponseEntity.ok(ApiResponse.ok("Черновики пользователя", drafts));
    }

    /**
     * Получить все опубликованные видео (публичный эндпоинт)
     */
    @GetMapping("/published")
    public ResponseEntity<ApiResponse<List<VideoResponse>>> getAllPublished() {
        List<VideoResponse> videos = videoService.getAllPublishedVideos();
        return ResponseEntity.ok(ApiResponse.ok("Опубликованные видео", videos));
    }
}
