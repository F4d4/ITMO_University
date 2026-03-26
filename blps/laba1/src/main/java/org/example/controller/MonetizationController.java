package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.MonetizationMethodRequest;
import org.example.dto.request.MonetizationRequest;
import org.example.dto.response.ApiResponse;
import org.example.dto.response.MonetizationMethodResponse;
import org.example.dto.response.MonetizationResponse;
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

    // ─── BPMN 3: Запрос на монетизацию ───────────────────────────────────────────

    /**
     * BPMN 3: Запрос на монетизацию видео.
     * Клиент (автор канала) выбирает стратегию монетизации и настраивает её.
     * Сервер проверяет соответствие: у пользователя должно быть
     * хотя бы одно опубликованное видео.
     * При успехе монетизация одобряется (статус APPROVED) и сохраняется в БД.
     * Клиент получает уведомление об одобрении.
     *
     * Параметры тела запроса:
     * - userId: ID пользователя
     * - videoId: ID видео (должно быть опубликовано)
     * - strategy: стратегия монетизации (например, "Монетизация через рекламу")
     * - configuration: описание конфигурации (опционально)
     *
     * Ответ: данные о монетизации со статусом APPROVED
     */
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<MonetizationResponse>> requestMonetization(
            @Valid @RequestBody MonetizationRequest request) {
        MonetizationResponse monetization = monetizationService.requestMonetization(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        "Монетизация одобрена. Уведомление об одобрении отправлено.",
                        monetization
                ));
    }

    // ─── BPMN 4: Добавление способа монетизации ──────────────────────────────────

    /**
     * BPMN 4: Добавление способа монетизации.
     * Клиент нажимает «Добавить рекламу» и выбирает тип.
     *
     * Сервер:
     * 1. Проверяет, что монетизация одобрена (APPROVED). Если нет → отказ.
     * 2. Если type = AD:
     *    - Требует adType (PRE_ROLL | MID_ROLL | POST_ROLL) и adName.
     *    - Проводит модерацию adName (проверка запрещённых слов). Если не прошло → отказ.
     * 3. Если type = SUBSCRIPTION:
     *    - Требует subscriptionPrice > 0.
     * 4. Сохраняет способ монетизации в БД.
     *
     * Параметры тела запроса:
     * - type: AD | SUBSCRIPTION
     * - adType: PRE_ROLL | MID_ROLL | POST_ROLL (только при type = AD)
     * - adName: название рекламы (только при type = AD, проходит модерацию)
     * - subscriptionPrice: цена подписки (только при type = SUBSCRIPTION)
     *
     * Ответ: данные о добавленном способе монетизации
     */
    @PostMapping("/{monetizationId}/methods")
    public ResponseEntity<ApiResponse<MonetizationMethodResponse>> addMonetizationMethod(
            @PathVariable Long monetizationId,
            @Valid @RequestBody MonetizationMethodRequest request) {
        MonetizationMethodResponse method = monetizationService.addMonetizationMethod(monetizationId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Способ монетизации успешно добавлен.", method));
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
     * Получить все монетизации пользователя
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<MonetizationResponse>>> getMonetizationsByUser(
            @PathVariable Long userId) {
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
