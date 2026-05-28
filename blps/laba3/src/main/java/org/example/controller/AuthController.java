package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.CreateUserRequest;
import org.example.dto.request.LoginRequest;
import org.example.dto.response.ApiResponse;
import org.example.dto.response.AuthResponse;
import org.example.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * BPMN 1: Регистрация нового пользователя.
     * Создаёт аккаунт с ролью USER и возвращает JWT токен.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody CreateUserRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Регистрация прошла успешно", response));
    }

    /**
     * BPMN 1: Авторизация пользователя.
     * Возвращает JWT токен для доступа к защищённым ресурсам.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Авторизация успешна", response));
    }
}
