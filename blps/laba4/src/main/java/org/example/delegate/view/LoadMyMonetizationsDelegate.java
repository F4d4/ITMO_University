package org.example.delegate.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.example.dto.response.MonetizationResponse;
import org.example.service.MonetizationService;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component("loadMyMonetizationsDelegate")
@RequiredArgsConstructor
public class LoadMyMonetizationsDelegate implements JavaDelegate {

    private final MonetizationService monetizationService;
    private final ObjectMapper objectMapper;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Long userId = ((Number) execution.getVariable("userId")).longValue();
        List<MonetizationResponse> list = monetizationService.getMonetizationsByUser(userId);
        execution.setVariable("monetizationListJson", objectMapper.writeValueAsString(list));
        execution.setVariable("refresh", false);
        log.info("Loaded {} monetizations for userId={}", list.size(), userId);
    }
}
