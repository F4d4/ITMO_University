package org.example.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.repository.MonetizationMethodRepository;
import org.example.repository.VideoRepository;
import org.example.service.MonetizationService;
import org.example.service.VideoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/bitrix")
@RequiredArgsConstructor
public class BitrixWebhookController {

    private final VideoRepository videoRepository;
    private final MonetizationMethodRepository monetizationMethodRepository;
    private final VideoService videoService;
    private final MonetizationService monetizationService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(HttpServletRequest request) {
        Map<String, String[]> params = request.getParameterMap();

        String event = getParam(params, "event");
        String taskIdStr = getParam(params, "data[FIELDS_AFTER][ID]");
        String statusStr = getParam(params, "data[FIELDS_AFTER][STATUS]");

        log.info("Bitrix24 webhook: event={}, taskId={}, status={}", event, taskIdStr, statusStr);

        if (!"ONTASKUPDATE".equals(event) || taskIdStr == null || statusStr == null) {
            return ResponseEntity.ok().build();
        }

        try {
            Long bitrixTaskId = Long.parseLong(taskIdStr);
            int status = Integer.parseInt(statusStr);

            if (status == 5) {
                processApproval(bitrixTaskId);
            } else if (status == 7) {
                processRejection(bitrixTaskId);
            }
        } catch (NumberFormatException e) {
            log.warn("Bitrix24 webhook: некорректные данные taskId={} status={}", taskIdStr, statusStr);
        }

        return ResponseEntity.ok().build();
    }

    private void processApproval(Long bitrixTaskId) {
        videoRepository.findByBitrixTaskId(bitrixTaskId).ifPresent(video -> {
            log.info("Bitrix24: одобрение публикации видео id={} (bitrixTask={})", video.getId(), bitrixTaskId);
            videoService.approvePublication(video.getId());
        });

        monetizationMethodRepository.findByBitrixTaskId(bitrixTaskId).ifPresent(method -> {
            log.info("Bitrix24: одобрение метода монетизации id={} (bitrixTask={})", method.getId(), bitrixTaskId);
            monetizationService.approveMonetizationMethod(method.getId());
        });
    }

    private void processRejection(Long bitrixTaskId) {
        videoRepository.findByBitrixTaskId(bitrixTaskId).ifPresent(video -> {
            log.info("Bitrix24: отклонение публикации видео id={} (bitrixTask={})", video.getId(), bitrixTaskId);
            videoService.rejectPublication(video.getId(), "Отклонено модератором в Bitrix24");
        });

        monetizationMethodRepository.findByBitrixTaskId(bitrixTaskId).ifPresent(method -> {
            log.info("Bitrix24: отклонение метода монетизации id={} (bitrixTask={})", method.getId(), bitrixTaskId);
            monetizationService.rejectMonetizationMethod(method.getId(), "Отклонено модератором в Bitrix24");
        });
    }

    private String getParam(Map<String, String[]> params, String key) {
        String[] values = params.get(key);
        return (values != null && values.length > 0) ? values[0] : null;
    }
}
