package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.example.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/moderation")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MODERATOR')")
public class ModerationController {

    private final TaskService taskService;
    private final RuntimeService runtimeService;

    @GetMapping("/videos/pending")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPendingVideos() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("assignee", "admin");

        ProcessInstance instance = runtimeService.startProcessInstanceByKey("moderation-pending-videos-process", variables);

        String pendingJson = (String) runtimeService.getVariable(instance.getId(), "pendingVideosJson");
        Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();

        Map<String, Object> result = new HashMap<>();
        result.put("processInstanceId", instance.getId());
        result.put("pendingVideosJson", pendingJson);
        if (task != null) {
            result.put("taskId", task.getId());
        }

        return ResponseEntity.ok(ApiResponse.ok("Видео на проверке", result));
    }

    @PostMapping("/videos/{videoId}/approve")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approveVideoPublication(
            @PathVariable Long videoId) {

        List<Task> tasks = taskService.createTaskQuery()
                .processVariableValueEquals("videoId", videoId)
                .taskDefinitionKey("reviewByModerator")
                .list();

        Map<String, Object> result = new HashMap<>();
        if (!tasks.isEmpty()) {
            Task task = tasks.get(0);
            Map<String, Object> vars = new HashMap<>();
            vars.put("moderatorApproved", true);
            vars.put("rejectionReason", "");
            taskService.complete(task.getId(), vars);
            result.put("taskId", task.getId());
            result.put("message", "Publication approved via Camunda process");
        } else {
            result.put("message", "No active moderation task found for videoId=" + videoId);
        }

        return ResponseEntity.ok(ApiResponse.ok("Публикация одобрена", result));
    }

    @PostMapping("/videos/{videoId}/reject")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rejectVideoPublication(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "Не соответствует правилам платформы") String reason) {

        List<Task> tasks = taskService.createTaskQuery()
                .processVariableValueEquals("videoId", videoId)
                .taskDefinitionKey("reviewByModerator")
                .list();

        Map<String, Object> result = new HashMap<>();
        if (!tasks.isEmpty()) {
            Task task = tasks.get(0);
            Map<String, Object> vars = new HashMap<>();
            vars.put("moderatorApproved", false);
            vars.put("rejectionReason", reason);
            taskService.complete(task.getId(), vars);
            result.put("taskId", task.getId());
            result.put("message", "Publication rejected via Camunda process");
        } else {
            result.put("message", "No active moderation task found for videoId=" + videoId);
        }

        return ResponseEntity.ok(ApiResponse.ok("Публикация отклонена. Причина: " + reason, result));
    }

    @GetMapping("/monetization-methods/pending")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPendingMethods() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("assignee", "admin");

        ProcessInstance instance = runtimeService.startProcessInstanceByKey("moderation-pending-methods-process", variables);

        String pendingJson = (String) runtimeService.getVariable(instance.getId(), "pendingMethodsJson");
        Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();

        Map<String, Object> result = new HashMap<>();
        result.put("processInstanceId", instance.getId());
        result.put("pendingMethodsJson", pendingJson);
        if (task != null) {
            result.put("taskId", task.getId());
        }

        return ResponseEntity.ok(ApiResponse.ok("Способы монетизации на проверке", result));
    }

    @PostMapping("/monetization-methods/{methodId}/approve")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approveMonetizationMethod(
            @PathVariable Long methodId) {

        List<Task> tasks = taskService.createTaskQuery()
                .processVariableValueEquals("methodId", methodId)
                .taskDefinitionKey("reviewAdMethod")
                .list();

        Map<String, Object> result = new HashMap<>();
        if (!tasks.isEmpty()) {
            Task task = tasks.get(0);
            Map<String, Object> vars = new HashMap<>();
            vars.put("adApproved", true);
            vars.put("adRejectionReason", "");
            taskService.complete(task.getId(), vars);
            result.put("taskId", task.getId());
            result.put("message", "Method approved via Camunda process");
        } else {
            result.put("message", "No active moderation task found for methodId=" + methodId);
        }

        return ResponseEntity.ok(ApiResponse.ok("Способ монетизации одобрен", result));
    }

    @PostMapping("/monetization-methods/{methodId}/reject")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rejectMonetizationMethod(
            @PathVariable Long methodId,
            @RequestParam(defaultValue = "Не соответствует правилам рекламы") String reason) {

        List<Task> tasks = taskService.createTaskQuery()
                .processVariableValueEquals("methodId", methodId)
                .taskDefinitionKey("reviewAdMethod")
                .list();

        Map<String, Object> result = new HashMap<>();
        if (!tasks.isEmpty()) {
            Task task = tasks.get(0);
            Map<String, Object> vars = new HashMap<>();
            vars.put("adApproved", false);
            vars.put("adRejectionReason", reason);
            taskService.complete(task.getId(), vars);
            result.put("taskId", task.getId());
            result.put("message", "Method rejected via Camunda process");
        } else {
            result.put("message", "No active moderation task found for methodId=" + methodId);
        }

        return ResponseEntity.ok(ApiResponse.ok("Способ монетизации отклонён. Причина: " + reason, result));
    }
}
