package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.task.Task;
import org.example.dto.response.ApiResponse;
import org.example.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/process")
@RequiredArgsConstructor
public class ProcessController {

    private final TaskService taskService;
    private final HistoryService historyService;
    private final SecurityUtils securityUtils;

    @GetMapping("/tasks/my")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyTasks() {
        Long userId = securityUtils.getCurrentUserId();
        String assignee = String.valueOf(userId);

        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(assignee)
                .list();

        List<Map<String, Object>> result = tasks.stream().map(this::taskToMap).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Задачи пользователя", result));
    }

    @GetMapping("/tasks/moderation")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getModerationTasks() {
        List<Task> tasks = taskService.createTaskQuery()
                .taskCandidateGroup("moderator")
                .list();

        List<Map<String, Object>> result = tasks.stream().map(this::taskToMap).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Задачи на модерацию", result));
    }

    @PostMapping("/tasks/{taskId}/complete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> completeTask(
            @PathVariable String taskId,
            @RequestBody(required = false) Map<String, Object> variables) {

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Задача не найдена: " + taskId));
        }

        taskService.complete(taskId, variables != null ? variables : new HashMap<>());

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("completed", true);
        return ResponseEntity.ok(ApiResponse.ok("Задача выполнена", result));
    }

    @GetMapping("/instance/{processInstanceId}/variables")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProcessVariables(
            @PathVariable String processInstanceId) {

        List<HistoricVariableInstance> vars = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .list();

        Map<String, Object> variables = new HashMap<>();
        vars.forEach(v -> variables.put(v.getName(), v.getValue()));

        return ResponseEntity.ok(ApiResponse.ok("Переменные процесса", variables));
    }

    private Map<String, Object> taskToMap(Task task) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", task.getId());
        map.put("name", task.getName());
        map.put("processInstanceId", task.getProcessInstanceId());
        map.put("assignee", task.getAssignee());
        map.put("created", task.getCreateTime());
        map.put("taskDefinitionKey", task.getTaskDefinitionKey());
        return map;
    }
}
