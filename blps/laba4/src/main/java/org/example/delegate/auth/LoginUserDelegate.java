package org.example.delegate.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.example.dto.request.LoginRequest;
import org.example.dto.response.AuthResponse;
import org.example.service.AuthService;
import org.springframework.stereotype.Component;

@Slf4j
@Component("loginUserDelegate")
@RequiredArgsConstructor
public class LoginUserDelegate implements JavaDelegate {

    private final AuthService authService;

    @Override
    public void execute(DelegateExecution execution) {
        String username = (String) execution.getVariable("username");
        String password = (String) execution.getVariable("password");

        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);

        AuthResponse response = authService.login(request);

        execution.setVariable("jwtToken", response.getToken());
        execution.setVariable("loggedInUserId", response.getUserId());
        execution.setVariable("authResultMessage", "Авторизация успешна. Пользователь: " + response.getUsername());
        log.info("User logged in via process: userId={}, username={}", response.getUserId(), response.getUsername());
    }
}
