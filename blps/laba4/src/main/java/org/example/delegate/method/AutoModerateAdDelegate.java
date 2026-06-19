package org.example.delegate.method;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.example.service.ValidationService;
import org.springframework.stereotype.Component;

@Slf4j
@Component("autoModerateAdDelegate")
@RequiredArgsConstructor
public class AutoModerateAdDelegate implements JavaDelegate {

    private final ValidationService validationService;

    @Override
    public void execute(DelegateExecution execution) {
        String adName = (String) execution.getVariable("adName");

        boolean clean = validationService.isAdNameClean(adName);

        execution.setVariable("adNameClean", clean);
        log.info("Ad name moderation: clean={}, adName={}", clean, adName);
    }
}
