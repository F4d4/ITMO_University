package org.example.service;

import jakarta.ejb.Singleton;
import jakarta.inject.Inject;
import org.example.aop.CacheStatistics;
import org.example.config.HibernateUtil;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Сервис для управления статистикой L2 Cache
 */
@Singleton
public class CacheStatisticsService {

    private static final Logger LOGGER = Logger.getLogger(CacheStatisticsService.class.getName());

    @Inject
    private HibernateUtil hibernateUtil;

    /**
     * Получить текущую статистику кэша
     */
    @CacheStatistics(enabled = false) // Не логируем статистику для этого метода
    public Map<String, Object> getCacheStatistics() {
        SessionFactory sessionFactory = hibernateUtil.getSessionFactory();
        Statistics stats = sessionFactory.getStatistics();

        Map<String, Object> result = new HashMap<>();
        
        // Общая статистика
        result.put("secondLevelCacheEnabled", true);
        result.put("statisticsEnabled", stats.isStatisticsEnabled());
        result.put("entityCacheHits", stats.getSecondLevelCacheHitCount());
        result.put("entityCacheMisses", stats.getSecondLevelCacheMissCount());
        result.put("entityCachePuts", stats.getSecondLevelCachePutCount());
        result.put("queryCacheHits", stats.getQueryCacheHitCount());
        result.put("queryCacheMisses", stats.getQueryCacheMissCount());
        result.put("queryCachePuts", stats.getQueryCachePutCount());

        // Hit ratio
        long totalHits = stats.getSecondLevelCacheHitCount();
        long totalMisses = stats.getSecondLevelCacheMissCount();
        long totalAccess = totalHits + totalMisses;
        
        if (totalAccess > 0) {
            double hitRatio = (double) totalHits / totalAccess * 100;
            result.put("hitRatio", String.format("%.2f%%", hitRatio));
        } else {
            result.put("hitRatio", "N/A");
        }

        // Статистика по регионам
        Map<String, Map<String, Long>> regions = new HashMap<>();
        String[] regionNames = stats.getSecondLevelCacheRegionNames();
        
        for (String regionName : regionNames) {
            Map<String, Long> regionStats = new HashMap<>();
            regionStats.put("hits", stats.getCacheRegionStatistics(regionName).getHitCount());
            regionStats.put("misses", stats.getCacheRegionStatistics(regionName).getMissCount());
            regionStats.put("puts", stats.getCacheRegionStatistics(regionName).getPutCount());
            regionStats.put("elementsInMemory", stats.getCacheRegionStatistics(regionName).getElementCountInMemory());
            regionStats.put("sizeInMemory", stats.getCacheRegionStatistics(regionName).getSizeInMemory());
            
            regions.put(regionName, regionStats);
        }
        
        result.put("regions", regions);

        return result;
    }

    /**
     * Сбросить статистику кэша
     */
    @CacheStatistics(enabled = false)
    public void clearStatistics() {
        SessionFactory sessionFactory = hibernateUtil.getSessionFactory();
        Statistics stats = sessionFactory.getStatistics();
        stats.clear();
        LOGGER.info("Статистика кэша сброшена");
    }

    /**
     * Очистить все кэши
     */
    @CacheStatistics(enabled = false)
    public void evictAllCaches() {
        SessionFactory sessionFactory = hibernateUtil.getSessionFactory();
        sessionFactory.getCache().evictAllRegions();
        LOGGER.info("Все кэши очищены");
    }

    /**
     * Очистить кэш конкретной сущности
     */
    @CacheStatistics(enabled = false)
    public void evictEntityCache(String entityName) {
        SessionFactory sessionFactory = hibernateUtil.getSessionFactory();
        sessionFactory.getCache().evictEntityData(entityName);
        LOGGER.info("Кэш сущности " + entityName + " очищен");
    }

    /**
     * Включить/выключить статистику
     */
    public void setStatisticsEnabled(boolean enabled) {
        SessionFactory sessionFactory = hibernateUtil.getSessionFactory();
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(enabled);
        LOGGER.info("Статистика кэша " + (enabled ? "включена" : "отключена"));
    }
}
