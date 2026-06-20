package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.example.dto.response.ApiResponse;
import org.example.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/monetization")
@RequiredArgsConstructor
public class MonetizationController {

    private final SecurityUtils securityUtils;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestMonetization(
            @RequestParam Long videoId) {

        Long userId = securityUtils.getCurrentUserId();
        String username = securityUtils.getCurrentUser().getUsername();

        Map<String, Object> variables = new HashMap<>();
        variables.put("videoId", videoId);
        variables.put("userId", userId);
        variables.put("uploaderId", username);
        variables.put("assignee", username);

        ProcessInstance instance = runtimeService.startProcessInstanceByKey("monetization-request-process", variables);

        Task task = taskService.createTaskQuery()
                .processInstanceId(instance.getId())
                .singleResult();

        Map<String, Object> result = new HashMap<>();
        result.put("processInstanceId", instance.getId());
        if (task != null) {
            result.put("taskId", task.getId());
            result.put("taskName", task.getName());
        } else {
            result.put("message", "Monetization request processed.");
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Запрос на монетизацию создан. Выберите стратегию в Camunda Tasklist.", result));
    }

    @PostMapping("/{monetizationId}/start-method")
    public ResponseEntity<ApiResponse<Map<String, Object>>> startAddMethod(
            @PathVariable Long monetizationId) {

        Long userId = securityUtils.getCurrentUserId();
        String username = securityUtils.getCurrentUser().getUsername();

        Map<String, Object> variables = new HashMap<>();
        variables.put("monetizationId", monetizationId);
        variables.put("userId", userId);
        variables.put("uploaderId", username);
        variables.put("assignee", username);

        ProcessInstance instance = runtimeService.startProcessInstanceByKey("monetization-method-process", variables);

        Task task = taskService.createTaskQuery()
                .processInstanceId(instance.getId())
                .singleResult();

        Map<String, Object> result = new HashMap<>();
        result.put("processInstanceId", instance.getId());
        if (task != null) {
            result.put("taskId", task.getId());
            result.put("taskName", task.getName());
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Процесс добавления способа монетизации запущен.", result));
    }

    @GetMapping("/{monetizationId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMonetizationById(
            @PathVariable Long monetizationId) {

        Long userId = securityUtils.getCurrentUserId();
        String username = securityUtils.getCurrentUser().getUsername();

        Map<String, Object> variables = new HashMap<>();
        variables.put("monetizationId", monetizationId);
        variables.put("userId", userId);
        variables.put("assignee", username);

        ProcessInstance instance = runtimeService.startProcessInstanceByKey("monetization-get-process", variables);

        String monetizationJson = (String) runtimeService.getVariable(instance.getId(), "monetizationJson");
        Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();

        Map<String, Object> result = new HashMap<>();
        result.put("processInstanceId", instance.getId());
        result.put("monetizationJson", monetizationJson);
        if (task != null) {
            result.put("taskId", task.getId());
        }

        return ResponseEntity.ok(ApiResponse.ok("Монетизация найдена", result));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyMonetizations() {
        Long userId = securityUtils.getCurrentUserId();
        String username = securityUtils.getCurrentUser().getUsername();

        Map<String, Object> variables = new HashMap<>();
        variables.put("userId", userId);
        variables.put("assignee", username);

        ProcessInstance instance = runtimeService.startProcessInstanceByKey("monetization-list-process", variables);

        String monetizationListJson = (String) runtimeService.getVariable(instance.getId(), "monetizationListJson");
        Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();

        Map<String, Object> result = new HashMap<>();
        result.put("processInstanceId", instance.getId());
        result.put("monetizationListJson", monetizationListJson);
        if (task != null) {
            result.put("taskId", task.getId());
        }

        return ResponseEntity.ok(ApiResponse.ok("Монетизации пользователя", result));
    }
}
