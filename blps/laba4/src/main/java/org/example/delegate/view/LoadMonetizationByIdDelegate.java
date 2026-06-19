package org.example.delegate.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.example.dto.response.MonetizationResponse;
import org.example.service.MonetizationService;
import org.springframework.stereotype.Component;

@Slf4j
@Component("loadMonetizationByIdDelegate")
@RequiredArgsConstructor
public class LoadMonetizationByIdDelegate implements JavaDelegate {

    private final MonetizationService monetizationService;
    private final ObjectMapper objectMapper;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Long monetizationId = ((Number) execution.getVariable("monetizationId")).longValue();
        MonetizationResponse monetization = monetizationService.getMonetizationById(monetizationId);
        execution.setVariable("monetizationJson", objectMapper.writeValueAsString(monetization));
        log.info("Loaded monetization id={}", monetizationId);
    }
}
