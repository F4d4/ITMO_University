package org.example.delegate.upload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.example.service.ValidationService;
import org.springframework.stereotype.Component;

@Slf4j
@Component("validateVideoFileDelegate")
@RequiredArgsConstructor
public class ValidateVideoFileDelegate implements JavaDelegate {

    private final ValidationService validationService;

    @Override
    public void execute(DelegateExecution execution) {
        Long fileSize = (Long) execution.getVariable("fileSize");
        String fileName = (String) execution.getVariable("fileName");

        boolean valid = validationService.isVideoFileMetaValid(fileName, fileSize);

        execution.setVariable("fileValid", valid);
        log.info("File validation: valid={}, fileName={}, size={}, pid={}", valid, fileName, fileSize, execution.getProcessInstanceId());
    }
}
