package org.example.delegate.method;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("addAdToDbDelegate")
public class AddAdToDbDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        Long methodId = ((Number) execution.getVariable("methodId")).longValue();
        log.info("AD method id={} record confirmed in DB as approved monetization method", methodId);
    }
}
