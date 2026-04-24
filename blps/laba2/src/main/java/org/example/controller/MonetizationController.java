package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.MonetizationMethodRequest;
import org.example.dto.request.MonetizationRequest;
import org.example.dto.response.ApiResponse;
import org.example.dto.response.MonetizationMethodResponse;
import org.example.dto.response.MonetizationResponse;
import org.example.security.SecurityUtils;
import org.example.service.MonetizationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monetization")
@RequiredArgsConstructor
public class MonetizationController {

    private final MonetizationService monetizationService;
    private final SecurityUtils securityUtils;

    // ─── BPMN 3: Запрос на монетизацию ───────────────────────────────────────────

    /**
     * BPMN 3: Запрос на монетизацию видео.
     * userId берётся из JWT токена.
     * Тело запроса: videoId, strategy, configuration.
     */
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<MonetizationResponse>> requestMonetization(
            @Valid @RequestBody MonetizationRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        MonetizationResponse monetization = monetizationService.requestMonetization(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        "Монетизация одобрена. Уведомление об одобрении отправлено.",
                        monetization
                ));
    }

    // ─── BPMN 4: Добавление способа монетизации ──────────────────────────────────

    /**
     * BPMN 4: Добавление способа монетизации (USER).
     * Сохраняется со статусом PENDING_REVIEW — ждёт проверки модератора.
     */
    @PostMapping("/{monetizationId}/methods")
    public ResponseEntity<ApiResponse<MonetizationMethodResponse>> addMonetizationMethod(
            @PathVariable Long monetizationId,
            @Valid @RequestBody MonetizationMethodRequest request) {
        MonetizationMethodResponse method = monetizationService.addMonetizationMethod(monetizationId, request);
        String message = method.getStatus() == org.example.entity.MethodStatus.APPROVED
                ? "Подписка автоматически одобрена системой."
                : "Реклама отправлена на проверку модератору.";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(message, method));
    }

    // ─── Вспомогательные эндпоинты ────────────────────────────────────────────────

    /**
     * Получить монетизацию по ID
     */
    @GetMapping("/{monetizationId}")
    public ResponseEntity<ApiResponse<MonetizationResponse>> getMonetizationById(
            @PathVariable Long monetizationId) {
        MonetizationResponse monetization = monetizationService.getMonetizationById(monetizationId);
        return ResponseEntity.ok(ApiResponse.ok("Монетизация найдена", monetization));
    }

    /**
     * Получить все монетизации для конкретного видео
     */
    @GetMapping("/video/{videoId}")
    public ResponseEntity<ApiResponse<List<MonetizationResponse>>> getMonetizationsByVideo(
            @PathVariable Long videoId) {
        List<MonetizationResponse> list = monetizationService.getMonetizationsByVideo(videoId);
        return ResponseEntity.ok(ApiResponse.ok("Монетизации для видео", list));
    }

    /**
     * Получить все монетизации текущего пользователя (userId из JWT)
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<MonetizationResponse>>> getMyMonetizations() {
        Long userId = securityUtils.getCurrentUserId();
        List<MonetizationResponse> list = monetizationService.getMonetizationsByUser(userId);
        return ResponseEntity.ok(ApiResponse.ok("Монетизации пользователя", list));
    }

    /**
     * Получить все способы монетизации для конкретной монетизации
     */
    @GetMapping("/{monetizationId}/methods")
    public ResponseEntity<ApiResponse<List<MonetizationMethodResponse>>> getMethodsByMonetization(
            @PathVariable Long monetizationId) {
        List<MonetizationMethodResponse> methods = monetizationService.getMethodsByMonetization(monetizationId);
        return ResponseEntity.ok(ApiResponse.ok("Способы монетизации", methods));
    }
}
