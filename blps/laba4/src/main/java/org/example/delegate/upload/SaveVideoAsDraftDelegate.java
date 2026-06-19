package org.example.delegate.upload;

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
@Component("saveVideoAsDraftDelegate")
@RequiredArgsConstructor
public class SaveVideoAsDraftDelegate implements JavaDelegate {

    private final VideoRepository videoRepository;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        Long videoId = ((Number) execution.getVariable("videoId")).longValue();
        String title = (String) execution.getVariable("title");
        String description = (String) execution.getVariable("description");
        String tags = (String) execution.getVariable("tags");

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Видео", videoId));

        video.setTitle(title);
        video.setDescription(description);
        video.setTags(tags);
        video.setStatus(VideoStatus.DRAFT);

        videoRepository.save(video);
        log.info("Video id={} saved as DRAFT with title='{}'", videoId, title);
    }
}
