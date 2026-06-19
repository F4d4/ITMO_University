package org.example.delegate.monetization;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.example.entity.Monetization;
import org.example.entity.MonetizationStatus;
import org.example.entity.User;
import org.example.entity.Video;
import org.example.exception.ResourceNotFoundException;
import org.example.repository.MonetizationRepository;
import org.example.repository.UserRepository;
import org.example.repository.VideoRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component("saveMonetizationDelegate")
@RequiredArgsConstructor
public class SaveMonetizationDelegate implements JavaDelegate {

    private final MonetizationRepository monetizationRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        Long videoId = ((Number) execution.getVariable("videoId")).longValue();
        Long userId = ((Number) execution.getVariable("userId")).longValue();
        String strategy = (String) execution.getVariable("strategy");
        String configuration = (String) execution.getVariable("configuration");

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Видео", videoId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", userId));

        Monetization monetization = new Monetization();
        monetization.setVideo(video);
        monetization.setUser(user);
        monetization.setStrategy(strategy != null ? strategy : "AD_BASED");
        monetization.setConfiguration(configuration);
        monetization.setStatus(MonetizationStatus.APPROVED);
        monetization.setProcessInstanceId(execution.getProcessInstanceId());

        monetization = monetizationRepository.save(monetization);
        execution.setVariable("monetizationId", monetization.getId());
        log.info("Monetization id={} saved for video id={}", monetization.getId(), videoId);
    }
}
