package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.example.dto.response.ApiResponse;
import org.example.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final SecurityUtils securityUtils;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserById(@PathVariable Long userId) {
        Long requesterId = securityUtils.getCurrentUserId();
        String username = securityUtils.getCurrentUser().getUsername();

        Map<String, Object> variables = new HashMap<>();
        variables.put("targetUserId", userId);
        variables.put("requesterId", requesterId);
        variables.put("assignee", username);

        ProcessInstance instance = runtimeService.startProcessInstanceByKey("user-get-process", variables);

        String userJson = (String) runtimeService.getVariable(instance.getId(), "userJson");
        Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();

        Map<String, Object> result = new HashMap<>();
        result.put("processInstanceId", instance.getId());
        result.put("userJson", userJson);
        if (task != null) {
            result.put("taskId", task.getId());
        }

        return ResponseEntity.ok(ApiResponse.ok("Пользователь найден", result));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllUsers() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("assignee", "admin");

        ProcessInstance instance = runtimeService.startProcessInstanceByKey("user-list-process", variables);

        String userListJson = (String) runtimeService.getVariable(instance.getId(), "userListJson");
        Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();

        Map<String, Object> result = new HashMap<>();
        result.put("processInstanceId", instance.getId());
        result.put("userListJson", userListJson);
        if (task != null) {
            result.put("taskId", task.getId());
        }

        return ResponseEntity.ok(ApiResponse.ok("Список пользователей", result));
    }
}
