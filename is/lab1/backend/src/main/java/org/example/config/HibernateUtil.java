package org.example.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.logging.Logger;

/**
 * Утилитный класс для управления Hibernate SessionFactory
 * Используется Hibernate Native API без JPA
 * Маппинг через XML файлы (.hbm.xml)
 * 
 * Использует прямое подключение к PostgreSQL через JDBC
 * (не через JNDI DataSource)
 */
@Singleton
@Startup
public class HibernateUtil {

    private static final Logger LOGGER = Logger.getLogger(HibernateUtil.class.getName());

    private SessionFactory sessionFactory;

    @PostConstruct
    public void init() {
        try {
            LOGGER.info("Инициализация Hibernate SessionFactory с DBCP2...");

            // Создаем конфигурацию Hibernate
            Configuration configuration = new Configuration();
            configuration.configure(); // Читает hibernate.cfg.xml из classpath

            sessionFactory = configuration.buildSessionFactory();

            LOGGER.info("Hibernate SessionFactory успешно инициализирована с DBCP2");
            LOGGER.info("DBCP2 Pool: initialSize=5, maxTotal=20, maxIdle=10, minIdle=5");

        } catch (Exception e) {
            LOGGER.severe("Ошибка при инициализации Hibernate SessionFactory: " + e.getMessage());
            e.printStackTrace();
            throw new ExceptionInInitializerError(e);
        }
    }

    public SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            throw new IllegalStateException("SessionFactory не инициализирована");
        }
        return sessionFactory;
    }

    @PreDestroy
    public void destroy() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
            LOGGER.info("Hibernate SessionFactory закрыта");
        }
    }
}
