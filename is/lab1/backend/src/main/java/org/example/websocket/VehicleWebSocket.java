package org.example.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * WebSocket endpoint для real-time уведомлений об изменениях Vehicle
 */
@ServerEndpoint("/ws/vehicles")
public class VehicleWebSocket {

    private static final Logger LOGGER = Logger.getLogger(VehicleWebSocket.class.getName());
    
    // Хранилище всех активных сессий
    private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        LOGGER.info("WebSocket подключен. ID сессии: " + session.getId() + 
                    ". Всего подключений: " + sessions.size());
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        LOGGER.info("WebSocket отключен. ID сессии: " + session.getId() + 
                    ". Осталось подключений: " + sessions.size());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        LOGGER.severe("WebSocket ошибка для сессии " + session.getId() + ": " + throwable.getMessage());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        LOGGER.info("Получено сообщение от клиента: " + message);
    }

    /**
     * Отправить уведомление всем подключенным клиентам
     */
    public static void broadcast(String message) {
        LOGGER.info("Отправка сообщения всем клиентам: " + message + 
                    ". Количество клиентов: " + sessions.size());
        
        synchronized (sessions) {
            for (Session session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.getBasicRemote().sendText(message);
                    } catch (IOException e) {
                        LOGGER.severe("Ошибка отправки сообщения клиенту " + 
                                      session.getId() + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Уведомить об удалении Vehicle
     */
    public static void notifyVehicleDeleted(Integer vehicleId) {
        String message = "{\"type\":\"DELETED\",\"id\":" + vehicleId + "}";
        broadcast(message);
    }

    /**
     * Уведомить о создании Vehicle
     */
    public static void notifyVehicleCreated(Integer vehicleId) {
        String message = "{\"type\":\"CREATED\",\"id\":" + vehicleId + "}";
        broadcast(message);
    }

    /**
     * Уведомить об обновлении Vehicle
     */
    public static void notifyVehicleUpdated(Integer vehicleId) {
        String message = "{\"type\":\"UPDATED\",\"id\":" + vehicleId + "}";
        broadcast(message);
    }
}

