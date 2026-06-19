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
@Component("changeVideoStatusPublishedDelegate")
@RequiredArgsConstructor
public class ChangeVideoStatusPublishedDelegate implements JavaDelegate {

    private final VideoRepository videoRepository;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        Long videoId = ((Number) execution.getVariable("videoId")).longValue();

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Видео", videoId));

        video.setStatus(VideoStatus.PUBLISHED);
        videoRepository.save(video);
        log.info("Video id={} status changed to PUBLISHED", videoId);
    }
}
