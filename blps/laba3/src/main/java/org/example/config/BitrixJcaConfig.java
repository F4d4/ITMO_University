package org.example.config;

import org.example.integration.bitrix.BitrixConnectionFactory;
import org.example.integration.bitrix.BitrixManagedConnectionFactory;
import org.example.integration.bitrix.BitrixResourceAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BitrixJcaConfig {

    @Value("${bitrix24.webhook-url}")
    private String webhookUrl;

    @Value("${bitrix24.responsible-id}")
    private String responsibleId;

    @Bean
    public BitrixResourceAdapter bitrixResourceAdapter() {
        return new BitrixResourceAdapter();
    }

    @Bean
    public BitrixManagedConnectionFactory bitrixManagedConnectionFactory() {
        BitrixManagedConnectionFactory mcf = new BitrixManagedConnectionFactory();
        mcf.setWebhookUrl(webhookUrl);
        mcf.setResponsibleId(responsibleId);
        return mcf;
    }

    @Bean
    public BitrixConnectionFactory bitrixConnectionFactory(BitrixManagedConnectionFactory mcf) {
        try {
            return (BitrixConnectionFactory) mcf.createConnectionFactory();
        } catch (jakarta.resource.ResourceException e) {
            throw new RuntimeException("Ошибка создания BitrixConnectionFactory", e);
        }
    }
}
