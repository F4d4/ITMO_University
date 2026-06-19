package org.example.delegate.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.example.dto.response.UserResponse;
import org.example.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Component("loadAllUsersDelegate")
@RequiredArgsConstructor
public class LoadAllUsersDelegate implements JavaDelegate {

    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Page<UserResponse> page = userService.getAllUsers(PageRequest.of(0, 50));
        execution.setVariable("userListJson", objectMapper.writeValueAsString(page.getContent()));
        execution.setVariable("refresh", false);
        log.info("Loaded {} users", page.getTotalElements());
    }
}
