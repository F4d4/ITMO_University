package org.example.delegate.publish;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.example.entity.AudienceType;
import org.example.entity.AccessType;
import org.example.entity.Video;
import org.example.entity.VideoStatus;
import org.example.exception.BusinessException;
import org.example.exception.ResourceNotFoundException;
import org.example.repository.VideoRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component("setPendingPublicationDelegate")
@RequiredArgsConstructor
public class SetPendingPublicationDelegate implements JavaDelegate {

    private final VideoRepository videoRepository;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        Long videoId = ((Number) execution.getVariable("videoId")).longValue();
        String audienceTypeStr = (String) execution.getVariable("audienceType");
        String accessTypeStr = (String) execution.getVariable("accessType");

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Видео", videoId));

        if (video.getStatus() != VideoStatus.DRAFT) {
            throw new BusinessException("Только черновик можно отправить на публикацию. Статус: " + video.getStatus());
        }

        if (video.getTitle() == null || video.getTitle().isBlank()) {
            throw new BusinessException("Нельзя опубликовать видео без названия");
        }

        video.setAudienceType(AudienceType.valueOf(audienceTypeStr));
        video.setAccessType(AccessType.valueOf(accessTypeStr));
        video.setStatus(VideoStatus.PENDING_PUBLICATION);
        video.setProcessInstanceId(execution.getProcessInstanceId());

        videoRepository.save(video);
        log.info("Video id={} status set to PENDING_PUBLICATION", videoId);
    }
}
