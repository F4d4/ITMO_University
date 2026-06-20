package org.example.delegate.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.example.entity.User;
import org.example.exception.BusinessException;
import org.example.repository.UserRepository;
import org.example.security.JwtTokenProvider;
import org.springframework.stereotype.Component;

/**
 * Execution listener на стартовом событии процессов, запускаемых из Camunda Tasklist.
 * Если в переменной jwtToken передан JWT — валидирует его, достаёт пользователя
 * и кладёт в процесс userId / username / uploaderId / assignee.
 * Если jwtToken отсутствует (процесс запущен через REST-контроллер), ничего не делает —
 * идентичность уже выставлена контроллером.
 */
@Slf4j
@Component("resolveJwtUserDelegate")
@RequiredArgsConstructor
public class ResolveJwtUserDelegate implements ExecutionListener {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public void notify(DelegateExecution execution) {
        String token = (String) execution.getVariable("jwtToken");
        if (token == null || token.isBlank()) {
            return;
        }
        if (!jwtTokenProvider.validateToken(token)) {
            throw new BusinessException("Недействительный или истёкший JWT токен");
        }
        String username = jwtTokenProvider.getUsernameFromToken(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Пользователь из токена не найден: " + username));

        execution.setVariable("userId", user.getId());
        execution.setVariable("username", username);
        execution.setVariable("uploaderId", username);
        execution.setVariable("assignee", username);
        log.info("JWT распознан: username={}, userId={}, pid={}", username, user.getId(), execution.getProcessInstanceId());
    }
}
