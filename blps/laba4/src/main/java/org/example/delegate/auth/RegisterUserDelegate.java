package org.example.delegate.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.example.dto.request.CreateUserRequest;
import org.example.dto.response.AuthResponse;
import org.example.service.AuthService;
import org.springframework.stereotype.Component;

@Slf4j
@Component("registerUserDelegate")
@RequiredArgsConstructor
public class RegisterUserDelegate implements JavaDelegate {

    private final AuthService authService;

    @Override
    public void execute(DelegateExecution execution) {
        String username = (String) execution.getVariable("username");
        String email = (String) execution.getVariable("email");
        String password = (String) execution.getVariable("password");
        String roleStr = (String) execution.getVariable("role");

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        if (roleStr != null) {
            request.setRole(org.example.entity.Role.valueOf(roleStr));
        }

        AuthResponse response = authService.register(request);

        execution.setVariable("jwtToken", response.getToken());
        execution.setVariable("registeredUserId", response.getUserId());
        execution.setVariable("authResultMessage", "Регистрация успешна. Пользователь: " + response.getUsername());
        log.info("User registered via process: userId={}, username={}", response.getUserId(), response.getUsername());
    }
}
