package org.example.delegate.upload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.variable.value.FileValue;
import org.camunda.bpm.engine.variable.value.TypedValue;
import org.example.service.ValidationService;
import org.springframework.stereotype.Component;

@Slf4j
@Component("validateFileFormatTaskListener")
@RequiredArgsConstructor
public class ValidateFileFormatTaskListener implements TaskListener {

    private final ValidationService validationService;

    @Override
    public void notify(DelegateTask delegateTask) {
        String fileName = resolveFileName(delegateTask);

        validationService.validateVideoFileFormat(fileName);

        delegateTask.setVariable("fileName", fileName);
        log.info("Формат файла принят task listener'ом: fileName={}, taskId={}, pid={}",
                fileName, delegateTask.getId(), delegateTask.getProcessInstanceId());
    }

    private String resolveFileName(DelegateTask delegateTask) {
        TypedValue typedValue = delegateTask.getVariableTyped("videoFile");
        if (typedValue instanceof FileValue fileValue) {
            return fileValue.getFilename();
        }
        return (String) delegateTask.getVariable("fileName");
    }
}
