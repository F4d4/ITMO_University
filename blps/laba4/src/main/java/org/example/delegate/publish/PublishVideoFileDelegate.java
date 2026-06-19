package org.example.delegate.publish;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.example.entity.Video;
import org.example.exception.ResourceNotFoundException;
import org.example.repository.VideoRepository;
import org.example.service.MinioService;
import org.springframework.stereotype.Component;

@Slf4j
@Component("publishVideoFileDelegate")
@RequiredArgsConstructor
public class PublishVideoFileDelegate implements JavaDelegate {

    private final VideoRepository videoRepository;
    private final MinioService minioService;

    @Override
    public void execute(DelegateExecution execution) {
        Long videoId = ((Number) execution.getVariable("videoId")).longValue();

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Видео", videoId));

        minioService.publishVideoFile(video.getMinioKey());
        log.info("Video file published to bucket={} for video id={}", minioService.getPublishedBucket(), videoId);
    }
}
