package org.example.delegate.upload;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.example.service.ValidationService;
import org.springframework.stereotype.Component;

@Slf4j
@Component("validateDescriptionDelegate")
public class ValidateDescriptionDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        String description = (String) execution.getVariable("description");

        boolean valid = description == null || description.length() <= ValidationService.MAX_DESCRIPTION_LENGTH;

        execution.setVariable("descriptionValid", valid);
        log.info("Description validation: valid={}, pid={}", valid, execution.getProcessInstanceId());
    }
}
