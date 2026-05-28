package org.example.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.simp.stomp.ReactorNettyTcpStompClient;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class StompMessageSender {

    private final ReactorNettyTcpStompClient stompClient;
    private final ObjectMapper objectMapper;

    @Value("${app.stomp.login:admin}")
    private String login;

    @Value("${app.stomp.passcode:admin}")
    private String passcode;

    public void sendPublicationTask(PublicationTaskMessage message) {
        StompSession session = null;
        try {
            StompHeaders connectHeaders = new StompHeaders();
            connectHeaders.setLogin(login);
            connectHeaders.setPasscode(passcode);

            session = stompClient.connectAsync(connectHeaders, new StompSessionHandlerAdapter() {})
                    .get(5, TimeUnit.SECONDS);

            String json = objectMapper.writeValueAsString(message);
            StompHeaders sendHeaders = new StompHeaders();
            sendHeaders.setDestination("/queue/publication-tasks");
            sendHeaders.setContentType(MimeType.valueOf("text/plain"));
            session.send(sendHeaders, json);

            log.info("STOMP: отправлено задание на публикацию видео id={}", message.getVideoId());
        } catch (Exception e) {
            log.error("STOMP: ошибка отправки задания на публикацию видео id={}: {}",
                    message.getVideoId(), e.getMessage(), e);
            throw new RuntimeException("Ошибка отправки STOMP сообщения", e);
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
}
