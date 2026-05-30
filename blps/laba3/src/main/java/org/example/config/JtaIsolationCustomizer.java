package org.example.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.jta.JtaTransactionManager;

@Component
public class JtaIsolationCustomizer {

    @Autowired
    private JtaTransactionManager transactionManager;

    @PostConstruct
    public void enableCustomIsolationLevels() {
        transactionManager.setAllowCustomIsolationLevels(true);
    }
}
