package org.example.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.ReactorNettyTcpStompClient;

@Configuration
public class StompClientConfig {

    @Value("${app.stomp.host}")
    private String host;

    @Value("${app.stomp.port}")
    private int port;

    @Bean
    public ReactorNettyTcpStompClient stompClient() {
        ReactorNettyTcpStompClient client = new ReactorNettyTcpStompClient(host, port);
        client.setMessageConverter(new StringMessageConverter());
        return client;
    }
}
