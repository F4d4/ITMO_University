package org.example.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StompMessageSender {

    @Value("${app.stomp.host}")
    private String host;

    @Value("${app.stomp.port}")
    private int port;

    @Value("${app.stomp.login:admin}")
    private String login;

    @Value("${app.stomp.passcode:admin}")
    private String passcode;

    private final ObjectMapper objectMapper;

    public void sendPublicationTask(PublicationTaskMessage message) {
        try (Socket socket = new Socket(host, port);
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), false);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            sendFrame(writer, "CONNECT", Map.of(
                    "login", login,
                    "passcode", passcode,
                    "accept-version", "1.0,1.1",
                    "host", host
            ), "");
            writer.flush();

            readUntilNull(reader);

            String body = objectMapper.writeValueAsString(message);
            sendFrame(writer, "SEND", Map.of(
                    "destination", "/queue/publication-tasks",
                    "content-type", "application/json",
                    "content-length", String.valueOf(body.getBytes(StandardCharsets.UTF_8).length)
            ), body);
            writer.flush();

            sendFrame(writer, "DISCONNECT", Map.of(), "");
            writer.flush();

            log.info("STOMP: отправлено задание на публикацию видео id={}", message.getVideoId());

        } catch (Exception e) {
            log.error("STOMP: ошибка отправки задания для видео id={}: {}", message.getVideoId(), e.getMessage(), e);
            throw new RuntimeException("Ошибка отправки STOMP сообщения", e);
        }
    }

    private void sendFrame(PrintWriter writer, String command, Map<String, String> headers, String body) {
        writer.print(command + "\n");
        headers.forEach((k, v) -> writer.print(k + ":" + v + "\n"));
        writer.print("\n");
        writer.print(body);
        writer.print("\0");
    }

    private void readUntilNull(BufferedReader reader) throws Exception {
        int ch;
        while ((ch = reader.read()) != -1) {
            if (ch == 0) break;
        }
    }
}
