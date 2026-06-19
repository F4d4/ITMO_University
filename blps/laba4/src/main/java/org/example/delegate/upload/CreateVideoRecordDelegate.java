package org.example.delegate.upload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.example.entity.User;
import org.example.entity.Video;
import org.example.entity.VideoStatus;
import org.example.exception.ResourceNotFoundException;
import org.example.repository.UserRepository;
import org.example.repository.VideoRepository;
import org.example.service.MinioService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component("createVideoRecordDelegate")
@RequiredArgsConstructor
public class CreateVideoRecordDelegate implements JavaDelegate {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final MinioService minioService;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        Long userId = ((Number) execution.getVariable("userId")).longValue();
        String minioKey = (String) execution.getVariable("minioKey");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", userId));

        Video video = new Video();
        video.setUser(user);
        video.setMinioKey(minioKey);
        video.setBucketName(minioService.getUploadedBucket());
        video.setStatus(VideoStatus.UPLOADING);
        video.setProcessInstanceId(execution.getProcessInstanceId());

        video = videoRepository.save(video);

        execution.setVariable("videoId", video.getId());
        log.info("Video record created: id={}, userId={}", video.getId(), userId);
    }
}
