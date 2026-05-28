package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.response.ApiResponse;
import org.example.dto.response.MonetizationMethodResponse;
import org.example.dto.response.VideoResponse;
import org.example.service.MonetizationService;
import org.example.service.VideoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/moderation")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MODERATOR')")
public class ModerationController {

    private final VideoService videoService;
    private final MonetizationService monetizationService;

    // ─── BPMN 2: Модерация видео ──────────────────────────────────────────────────

    /**
     * BPMN 2 (MODERATOR): Получить список видео, ожидающих публикации.
     */
    @GetMapping("/videos/pending")
    public ResponseEntity<ApiResponse<List<VideoResponse>>> getPendingVideos() {
        List<VideoResponse> videos = videoService.getPendingPublicationVideos();
        return ResponseEntity.ok(ApiResponse.ok("Видео на проверке", videos));
    }

    /**
     * BPMN 2 (MODERATOR): Одобрить публикацию видео.
     * Программная JTA-транзакция (Narayana):
     *   - Изменить статус видео на PUBLISHED
     *   - Сохранить параметры публикации
     *   - Добавить запись в БД опубликованных видео
     */
    @PostMapping("/videos/{videoId}/approve")
    public ResponseEntity<ApiResponse<VideoResponse>> approveVideoPublication(
            @PathVariable Long videoId) {
        VideoResponse video = videoService.approvePublication(videoId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Публикация видео одобрена. Видео опубликовано.", video));
    }

    /**
     * BPMN 2 (MODERATOR): Отклонить публикацию видео.
     * Уведомление об отказе отправляется пользователю.
     */
    @PostMapping("/videos/{videoId}/reject")
    public ResponseEntity<ApiResponse<VideoResponse>> rejectVideoPublication(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "Не соответствует правилам платформы") String reason) {
        VideoResponse video = videoService.rejectPublication(videoId, reason);
        return ResponseEntity.ok(ApiResponse.ok(
                "Публикация видео отклонена. Причина: " + reason, video));
    }

    // ─── BPMN 4: Модерация способов монетизации ───────────────────────────────────

    /**
     * BPMN 4 (MODERATOR): Получить список способов монетизации, ожидающих проверки.
     */
    @GetMapping("/monetization-methods/pending")
    public ResponseEntity<ApiResponse<List<MonetizationMethodResponse>>> getPendingMethods() {
        List<MonetizationMethodResponse> methods = monetizationService.getPendingMethods();
        return ResponseEntity.ok(ApiResponse.ok("Способы монетизации на проверке", methods));
    }

    /**
     * BPMN 4 (MODERATOR): Одобрить способ монетизации.
     * Программная JTA-транзакция (Narayana):
     *   - Сохранить способы монетизации (статус → APPROVED)
     *   - Создать теги
     *   - Добавить запись в БД монетизаций
     */
    @PostMapping("/monetization-methods/{methodId}/approve")
    public ResponseEntity<ApiResponse<MonetizationMethodResponse>> approveMonetizationMethod(
            @PathVariable Long methodId) {
        MonetizationMethodResponse method = monetizationService.approveMonetizationMethod(methodId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Способ монетизации одобрен.", method));
    }

    /**
     * BPMN 4 (MODERATOR): Отклонить способ монетизации.
     */
    @PostMapping("/monetization-methods/{methodId}/reject")
    public ResponseEntity<ApiResponse<MonetizationMethodResponse>> rejectMonetizationMethod(
            @PathVariable Long methodId,
            @RequestParam(defaultValue = "Не соответствует правилам рекламы") String reason) {
        MonetizationMethodResponse method = monetizationService.rejectMonetizationMethod(methodId, reason);
        return ResponseEntity.ok(ApiResponse.ok(
                "Способ монетизации отклонён. Причина: " + reason, method));
    }
}
