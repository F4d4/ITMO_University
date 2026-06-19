package org.example.delegate.upload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.example.service.MinioService;
import org.springframework.stereotype.Component;

@Slf4j
@Component("cleanupInvalidFileDelegate")
@RequiredArgsConstructor
public class CleanupInvalidFileDelegate implements JavaDelegate {

    private final MinioService minioService;

    @Override
    public void execute(DelegateExecution execution) {
        String minioKey = (String) execution.getVariable("minioKey");
        if (minioKey != null) {
            try {
                minioService.deleteObject(minioService.getUploadedBucket(), minioKey);
                log.info("Cleaned up invalid file from MinIO: {}", minioKey);
            } catch (Exception e) {
                log.warn("Failed to cleanup MinIO file {}: {}", minioKey, e.getMessage());
            }
        }
    }
}
