package org.example.delegate.publish;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.example.entity.Video;
import org.example.entity.VideoStatus;
import org.example.exception.ResourceNotFoundException;
import org.example.repository.VideoRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component("rejectPublicationDelegate")
@RequiredArgsConstructor
public class RejectPublicationDelegate implements JavaDelegate {

    private final VideoRepository videoRepository;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        Long videoId = ((Number) execution.getVariable("videoId")).longValue();
        String rejectionReason = (String) execution.getVariable("rejectionReason");

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Видео", videoId));

        video.setStatus(VideoStatus.DRAFT);
        videoRepository.save(video);
        log.info("Publication rejected for video id={}, reason: {}", videoId, rejectionReason);
    }
}
