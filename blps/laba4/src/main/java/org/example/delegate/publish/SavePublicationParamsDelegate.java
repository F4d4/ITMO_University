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
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component("savePublicationParamsDelegate")
@RequiredArgsConstructor
public class SavePublicationParamsDelegate implements JavaDelegate {

    private final VideoRepository videoRepository;
    private final MinioService minioService;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        Long videoId = ((Number) execution.getVariable("videoId")).longValue();

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Видео", videoId));

        video.setBucketName(minioService.getPublishedBucket());
        videoRepository.save(video);
        log.info("Publication params saved for video id={}, bucket={}", videoId, minioService.getPublishedBucket());
    }
}
