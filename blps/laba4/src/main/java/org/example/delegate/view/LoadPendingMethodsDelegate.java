package org.example.delegate.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.example.dto.response.MonetizationMethodResponse;
import org.example.service.MonetizationService;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component("loadPendingMethodsDelegate")
@RequiredArgsConstructor
public class LoadPendingMethodsDelegate implements JavaDelegate {

    private final MonetizationService monetizationService;
    private final ObjectMapper objectMapper;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        List<MonetizationMethodResponse> methods = monetizationService.getPendingMethods();
        execution.setVariable("pendingMethodsJson", objectMapper.writeValueAsString(methods));
        execution.setVariable("refresh", false);
        log.info("Loaded {} pending methods for moderation", methods.size());
    }
}
