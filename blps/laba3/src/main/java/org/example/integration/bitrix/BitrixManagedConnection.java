package org.example.integration.bitrix;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionEvent;
import jakarta.resource.spi.ConnectionEventListener;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.LocalTransaction;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionMetaData;
import javax.transaction.xa.XAResource;
import lombok.extern.slf4j.Slf4j;

import javax.security.auth.Subject;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class BitrixManagedConnection implements ManagedConnection {

    private final String webhookUrl;
    private final String responsibleId;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final List<ConnectionEventListener> listeners = new ArrayList<>();
    private BitrixConnectionImpl connectionHandle;

    public BitrixManagedConnection(String webhookUrl, String responsibleId) {
        this.webhookUrl = webhookUrl;
        this.responsibleId = responsibleId;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        connectionHandle = new BitrixConnectionImpl(this);
        return connectionHandle;
    }

    @Override
    public void destroy() throws ResourceException {
        connectionHandle = null;
    }

    @Override
    public void cleanup() throws ResourceException {
        if (connectionHandle != null) {
            connectionHandle.invalidate();
            connectionHandle = null;
        }
    }

    @Override
    public void associateConnection(Object connection) throws ResourceException {
        if (connection instanceof BitrixConnectionImpl) {
            connectionHandle = (BitrixConnectionImpl) connection;
            connectionHandle.setManagedConnection(this);
        }
    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public XAResource getXAResource() throws ResourceException {
        return null;
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        return null;
    }

    @Override
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        return new ManagedConnectionMetaData() {
            @Override public String getEISProductName() { return "Bitrix24"; }
            @Override public String getEISProductVersion() { return "REST API 1.0"; }
            @Override public int getMaxConnections() { return 10; }
            @Override public String getUserName() { return "webhook"; }
        };
    }

    @Override
    public void setLogWriter(PrintWriter out) throws ResourceException {
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return null;
    }

    public Long createTask(String title, String description) {
        try {
            Map<String, Object> fields = Map.of(
                    "TITLE", title,
                    "DESCRIPTION", description,
                    "RESPONSIBLE_ID", responsibleId
            );
            String body = objectMapper.writeValueAsString(Map.of("fields", fields));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl + "tasks.task.add.json"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Bitrix24 вернул статус {}: {}", response.statusCode(), response.body());
                return null;
            }

            JsonNode json = objectMapper.readTree(response.body());
            long taskId = json.path("result").path("task").path("id").asLong(0);
            return taskId > 0 ? taskId : null;

        } catch (Exception e) {
            log.error("Ошибка создания задачи в Bitrix24: {}", e.getMessage(), e);
            return null;
        }
    }

    public Integer getTaskStatus(Long taskId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl + "tasks.task.get.json?taskId=" + taskId))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Bitrix24 tasks.task.get вернул статус {}", response.statusCode());
                return null;
            }

            JsonNode json = objectMapper.readTree(response.body());
            String statusStr = json.path("result").path("task").path("status").asText(null);
            return statusStr != null ? Integer.parseInt(statusStr) : null;

        } catch (Exception e) {
            log.error("Ошибка получения статуса задачи Bitrix24 id={}: {}", taskId, e.getMessage(), e);
            return null;
        }
    }

    void notifyConnectionClosed() {
        ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
        event.setConnectionHandle(connectionHandle);
        listeners.forEach(l -> l.connectionClosed(event));
    }
}
