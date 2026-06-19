package org.example.delegate.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.example.dto.response.VideoResponse;
import org.example.service.VideoService;
import org.springframework.stereotype.Component;

@Slf4j
@Component("loadVideoByIdDelegate")
@RequiredArgsConstructor
public class LoadVideoByIdDelegate implements JavaDelegate {

    private final VideoService videoService;
    private final ObjectMapper objectMapper;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Long videoId = ((Number) execution.getVariable("videoId")).longValue();
        VideoResponse video = videoService.getVideoById(videoId);
        execution.setVariable("videoJson", objectMapper.writeValueAsString(video));
        log.info("Loaded video id={}", videoId);
    }
}
