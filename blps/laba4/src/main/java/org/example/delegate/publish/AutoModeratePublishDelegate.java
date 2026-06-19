package org.example.delegate.publish;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.example.entity.Video;
import org.example.exception.ResourceNotFoundException;
import org.example.repository.VideoRepository;
import org.example.service.ValidationService;
import org.springframework.stereotype.Component;

@Slf4j
@Component("autoModeratePublishDelegate")
@RequiredArgsConstructor
public class AutoModeratePublishDelegate implements JavaDelegate {

    private final VideoRepository videoRepository;
    private final ValidationService validationService;

    @Override
    public void execute(DelegateExecution execution) {
        Long videoId = ((Number) execution.getVariable("videoId")).longValue();

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Видео", videoId));

        boolean autoApproved = validationService.isDescriptionClean(video.getDescription());

        execution.setVariable("autoApproved", autoApproved);
        log.info("Auto-moderation for video id={}: autoApproved={}", videoId, autoApproved);
    }
}
