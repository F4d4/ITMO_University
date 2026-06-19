package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.example.dto.request.CreateUserRequest;
import org.example.dto.request.LoginRequest;
import org.example.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RuntimeService runtimeService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @Valid @RequestBody CreateUserRequest request) {

        Map<String, Object> variables = new HashMap<>();
        variables.put("username", request.getUsername());
        variables.put("email", request.getEmail());
        variables.put("password", request.getPassword());

        ProcessInstance instance = runtimeService.startProcessInstanceByKey("auth-register-process", variables);

        String token = (String) runtimeService.getVariable(instance.getId(), "jwtToken");
        Object userId = runtimeService.getVariable(instance.getId(), "registeredUserId");

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("tokenType", "Bearer");
        result.put("userId", userId);
        result.put("username", request.getUsername());
        result.put("processInstanceId", instance.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Регистрация прошла успешно", result));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody LoginRequest request) {

        Map<String, Object> variables = new HashMap<>();
        variables.put("username", request.getUsername());
        variables.put("password", request.getPassword());

        ProcessInstance instance = runtimeService.startProcessInstanceByKey("auth-login-process", variables);

        String token = (String) runtimeService.getVariable(instance.getId(), "jwtToken");
        Object userId = runtimeService.getVariable(instance.getId(), "loggedInUserId");

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("tokenType", "Bearer");
        result.put("userId", userId);
        result.put("username", request.getUsername());
        result.put("processInstanceId", instance.getId());

        return ResponseEntity.ok(ApiResponse.ok("Авторизация успешна", result));
    }
}
