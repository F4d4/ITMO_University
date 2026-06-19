package org.example.delegate.monetization;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.example.entity.MonetizationStatus;
import org.example.entity.VideoStatus;
import org.example.repository.MonetizationRepository;
import org.example.repository.VideoRepository;
import org.springframework.stereotype.Component;

@Slf4j
@Component("checkMonetizationEligibilityDelegate")
@RequiredArgsConstructor
public class CheckMonetizationEligibilityDelegate implements JavaDelegate {

    private final VideoRepository videoRepository;
    private final MonetizationRepository monetizationRepository;

    @Override
    public void execute(DelegateExecution execution) {
        Long videoId = ((Number) execution.getVariable("videoId")).longValue();
        Long userId = ((Number) execution.getVariable("userId")).longValue();

        boolean eligible = false;

        var videoOpt = videoRepository.findById(videoId);
        if (videoOpt.isPresent()) {
            var video = videoOpt.get();
            boolean videoPublished = video.getStatus() == VideoStatus.PUBLISHED;
            boolean videoOwnedByUser = video.getUser().getId().equals(userId);
            boolean userHasPublished = videoRepository.existsByUserIdAndStatus(userId, VideoStatus.PUBLISHED);
            boolean noExistingApproved = !monetizationRepository.existsByVideoIdAndStatus(videoId, MonetizationStatus.APPROVED);

            eligible = videoPublished && videoOwnedByUser && userHasPublished && noExistingApproved;
            log.info("Eligibility check: videoPublished={}, owned={}, hasPublished={}, noExisting={}", videoPublished, videoOwnedByUser, userHasPublished, noExistingApproved);
        }

        execution.setVariable("eligible", eligible);
        log.info("Monetization eligibility: eligible={}, videoId={}, userId={}", eligible, videoId, userId);
    }
}
