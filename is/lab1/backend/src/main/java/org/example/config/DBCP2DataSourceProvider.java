package org.example.config;

import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;

/**
 * Конфигурация Apache Commons DBCP2 DataSource
 * Программная настройка пула соединений
 */
public class DBCP2DataSourceProvider {

    private static BasicDataSource dataSource;

    /**
     * Получить настроенный DBCP2 DataSource
     */
    public static synchronized DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new BasicDataSource();
            
            // Основные параметры подключения
            dataSource.setDriverClassName("org.postgresql.Driver");
            dataSource.setUrl("jdbc:postgresql://localhost:5432/test");
            dataSource.setUsername("postgres");
            dataSource.setPassword("KosPrav1979");
            
            // Параметры пула DBCP2
            // Начальное количество соединений при инициализации
            dataSource.setInitialSize(5);
            
            // Максимальное количество активных соединений
            dataSource.setMaxTotal(20);
            
            // Максимальное количество простаивающих соединений
            dataSource.setMaxIdle(10);
            
            // Минимальное количество простаивающих соединений
            dataSource.setMinIdle(5);
            
            // Максимальное время ожидания соединения (30 секунд)
            dataSource.setMaxWaitMillis(30000);
            
            // Проверка соединения перед выдачей из пула
            dataSource.setTestOnBorrow(true);
            
            // Проверка простаивающих соединений
            dataSource.setTestWhileIdle(true);
            
            // SQL запрос для проверки валидности соединения
            dataSource.setValidationQuery("SELECT 1");
            
            // Интервал проверки простаивающих соединений (60 секунд)
            dataSource.setTimeBetweenEvictionRunsMillis(60000);
            
            // Минимальное время простоя соединения перед вытеснением (5 минут)
            dataSource.setMinEvictableIdleTimeMillis(300000);
            
            // Удалять брошенные соединения
            dataSource.setRemoveAbandonedOnBorrow(true);
            dataSource.setRemoveAbandonedTimeout(300);
            
            // Логирование брошенных соединений
            dataSource.setLogAbandoned(true);
        }
        
        return dataSource;
    }
    
    /**
     * Закрыть DataSource
     */
    public static synchronized void closeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            try {
                dataSource.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
