package org.example.delegate.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.example.dto.response.UserResponse;
import org.example.service.UserService;
import org.springframework.stereotype.Component;

@Slf4j
@Component("loadUserByIdDelegate")
@RequiredArgsConstructor
public class LoadUserByIdDelegate implements JavaDelegate {

    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Long targetUserId = ((Number) execution.getVariable("targetUserId")).longValue();
        UserResponse user = userService.getUserById(targetUserId);
        execution.setVariable("userJson", objectMapper.writeValueAsString(user));
        log.info("Loaded user id={}", targetUserId);
    }
}
