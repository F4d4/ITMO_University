package org.example.delegate.upload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.variable.value.FileValue;
import org.example.exception.BusinessException;
import org.example.service.MinioService;
import org.example.service.ValidationService;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Component("validateVideoFileDelegate")
@RequiredArgsConstructor
public class ValidateVideoFileDelegate implements JavaDelegate {

    private final ValidationService validationService;
    private final MinioService minioService;

    @Override
    public void execute(DelegateExecution execution) {
        // Если процесс запущен из Camunda Tasklist, файл приходит как переменная FileValue,
        // а minioKey ещё не установлен (его выставлял REST-контроллер). Загружаем сами.
        if (execution.getVariable("minioKey") == null) {
            FileValue fileValue = execution.getVariableTyped("videoFile");
            if (fileValue == null) {
                throw new BusinessException("Файл видео не передан (переменная videoFile)");
            }
            byte[] bytes;
            try (InputStream is = fileValue.getValue()) {
                bytes = is.readAllBytes();
            } catch (Exception e) {
                throw new BusinessException("Ошибка чтения файла: " + e.getMessage());
            }
            String objectName = "video_" + UUID.randomUUID() + ".mp4";
            minioService.uploadVideoFile(new ByteArrayInputStream(bytes), bytes.length, objectName);
            execution.setVariable("minioKey", objectName);
            execution.setVariable("fileName", fileValue.getFilename());
            execution.setVariable("fileSize", (long) bytes.length);
        }

        Long fileSize = (Long) execution.getVariable("fileSize");
        String fileName = (String) execution.getVariable("fileName");

        boolean valid = validationService.isVideoFileMetaValid(fileName, fileSize);

        execution.setVariable("fileValid", valid);
        log.info("File validation: valid={}, fileName={}, size={}, pid={}", valid, fileName, fileSize, execution.getProcessInstanceId());
    }
}
