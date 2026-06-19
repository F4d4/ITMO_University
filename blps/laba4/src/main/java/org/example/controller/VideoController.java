package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.example.dto.response.ApiResponse;
import org.example.security.SecurityUtils;
import org.example.service.MinioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final SecurityUtils securityUtils;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final MinioService minioService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadVideo(
            @RequestParam("file") MultipartFile file) {

        Long userId = securityUtils.getCurrentUserId();
        String objectName = "video_" + UUID.randomUUID() + ".mp4";
        minioService.uploadVideoFile(file, objectName);

        Map<String, Object> variables = new HashMap<>();
        variables.put("userId", userId);
        variables.put("uploaderId", String.valueOf(userId));
        variables.put("assignee", String.valueOf(userId));
        variables.put("minioKey", objectName);
        variables.put("fileSize", file.getSize());
        variables.put("fileName", file.getOriginalFilename() != null ? file.getOriginalFilename() : objectName);

        ProcessInstance instance = runtimeService.startProcessInstanceByKey("video-upload-process", variables);

        Task task = taskService.createTaskQuery()
                .processInstanceId(instance.getId())
                .singleResult();

        Map<String, Object> result = new HashMap<>();
        result.put("processInstanceId", instance.getId());
        if (task != null) {
            result.put("taskId", task.getId());
            result.put("taskName", task.getName());
        } else {
            result.put("message", "File validation failed.");
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Файл принят. Заполните информацию о видео в Camunda Tasklist.", result));
    }

    @PostMapping("/{videoId}/start-publish")
    public ResponseEntity<ApiResponse<Map<String, Object>>> startPublish(@PathVariable Long videoId) {
        Long userId = securityUtils.getCurrentUserId();

        Map<String, Object> variables = new HashMap<>();
        variables.put("videoId", videoId);
        variables.put("userId", userId);
        variables.put("uploaderId", String.valueOf(userId));
        variables.put("assignee", String.valueOf(userId));

        ProcessInstance instance = runtimeService.startProcessInstanceByKey("video-publish-process", variables);

        Task task = taskService.createTaskQuery()
                .processInstanceId(instance.getId())
                .singleResult();

        Map<String, Object> result = new HashMap<>();
        result.put("processInstanceId", instance.getId());
        if (task != null) {
            result.put("taskId", task.getId());
            result.put("taskName", task.getName());
        }

        return ResponseEntity.ok(ApiResponse.ok("Процесс публикации запущен. Настройте параметры в Camunda Tasklist.", result));
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVideoById(@PathVariable Long videoId) {
        Long userId = securityUtils.getCurrentUserId();

        Map<String, Object> variables = new HashMap<>();
        variables.put("videoId", videoId);
        variables.put("userId", userId);
        variables.put("assignee", String.valueOf(userId));

        ProcessInstance instance = runtimeService.startProcessInstanceByKey("video-get-process", variables);

        String videoJson = (String) runtimeService.getVariable(instance.getId(), "videoJson");
        Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();

        Map<String, Object> result = new HashMap<>();
        result.put("processInstanceId", instance.getId());
        result.put("videoJson", videoJson);
        if (task != null) {
            result.put("taskId", task.getId());
        }

        return ResponseEntity.ok(ApiResponse.ok("Видео найдено", result));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyVideos() {
        Long userId = securityUtils.getCurrentUserId();

        Map<String, Object> variables = new HashMap<>();
        variables.put("userId", userId);
        variables.put("assignee", String.valueOf(userId));

        ProcessInstance instance = runtimeService.startProcessInstanceByKey("video-list-process", variables);

        String videoListJson = (String) runtimeService.getVariable(instance.getId(), "videoListJson");
        Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();

        Map<String, Object> result = new HashMap<>();
        result.put("processInstanceId", instance.getId());
        result.put("videoListJson", videoListJson);
        if (task != null) {
            result.put("taskId", task.getId());
        }

        return ResponseEntity.ok(ApiResponse.ok("Список видео пользователя", result));
    }

    @GetMapping("/drafts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyDrafts() {
        Long userId = securityUtils.getCurrentUserId();

        Map<String, Object> variables = new HashMap<>();
        variables.put("userId", userId);
        variables.put("assignee", String.valueOf(userId));

        ProcessInstance instance = runtimeService.startProcessInstanceByKey("video-drafts-process", variables);

        String draftListJson = (String) runtimeService.getVariable(instance.getId(), "draftListJson");
        Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();

        Map<String, Object> result = new HashMap<>();
        result.put("processInstanceId", instance.getId());
        result.put("draftListJson", draftListJson);
        if (task != null) {
            result.put("taskId", task.getId());
        }

        return ResponseEntity.ok(ApiResponse.ok("Черновики пользователя", result));
    }

    @GetMapping("/published")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllPublished() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("assignee", "admin");

        ProcessInstance instance = runtimeService.startProcessInstanceByKey("video-published-list-process", variables);

        String publishedListJson = (String) runtimeService.getVariable(instance.getId(), "publishedListJson");
        Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();

        Map<String, Object> result = new HashMap<>();
        result.put("processInstanceId", instance.getId());
        result.put("publishedListJson", publishedListJson);
        if (task != null) {
            result.put("taskId", task.getId());
        }

        return ResponseEntity.ok(ApiResponse.ok("Опубликованные видео", result));
    }
}
