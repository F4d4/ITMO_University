package org.example.delegate.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.example.dto.response.VideoResponse;
import org.example.service.VideoService;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component("loadPendingVideosDelegate")
@RequiredArgsConstructor
public class LoadPendingVideosDelegate implements JavaDelegate {

    private final VideoService videoService;
    private final ObjectMapper objectMapper;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        List<VideoResponse> videos = videoService.getPendingPublicationVideos();
        execution.setVariable("pendingVideosJson", objectMapper.writeValueAsString(videos));
        execution.setVariable("refresh", false);
        log.info("Loaded {} pending videos for moderation", videos.size());
    }
}
