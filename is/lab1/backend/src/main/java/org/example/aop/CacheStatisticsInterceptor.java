package org.example.aop;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.example.config.HibernateUtil;

import jakarta.inject.Inject;
import java.util.logging.Logger;

/**
 * CDI Interceptor для логирования статистики L2 Cache
 * 
 * Перехватывает методы, помеченные аннотацией @CacheStatistics,
 * и выводит статистику использования кэша (cache hits, cache misses)
 */
@CacheStatistics
@Interceptor
public class CacheStatisticsInterceptor {

    private static final Logger LOGGER = Logger.getLogger(CacheStatisticsInterceptor.class.getName());

    @Inject
    private HibernateUtil hibernateUtil;

    @AroundInvoke
    public Object logCacheStatistics(InvocationContext context) throws Exception {
        // Проверяем, включено ли логирование
        CacheStatistics annotation = context.getMethod().getAnnotation(CacheStatistics.class);
        if (annotation == null) {
            annotation = context.getTarget().getClass().getAnnotation(CacheStatistics.class);
        }

        boolean enabled = annotation != null && annotation.enabled();

        if (!enabled) {
            // Логирование отключено - просто выполняем метод
            return context.proceed();
        }

        // Получаем статистику ДО выполнения метода
        SessionFactory sessionFactory = hibernateUtil.getSessionFactory();
        Statistics stats = sessionFactory.getStatistics();
        
        long hitCountBefore = stats.getSecondLevelCacheHitCount();
        long missCountBefore = stats.getSecondLevelCacheMissCount();
        long putCountBefore = stats.getSecondLevelCachePutCount();
        long queryHitCountBefore = stats.getQueryCacheHitCount();
        long queryMissCountBefore = stats.getQueryCacheMissCount();

        // Выполняем целевой метод
        Object result = context.proceed();

        // Получаем статистику ПОСЛЕ выполнения метода
        long hitCountAfter = stats.getSecondLevelCacheHitCount();
        long missCountAfter = stats.getSecondLevelCacheMissCount();
        long putCountAfter = stats.getSecondLevelCachePutCount();
        long queryHitCountAfter = stats.getQueryCacheHitCount();
        long queryMissCountAfter = stats.getQueryCacheMissCount();

        // Вычисляем разницу
        long hits = hitCountAfter - hitCountBefore;
        long misses = missCountAfter - missCountBefore;
        long puts = putCountAfter - putCountBefore;
        long queryHits = queryHitCountAfter - queryHitCountBefore;
        long queryMisses = queryMissCountAfter - queryMissCountBefore;

        // Логируем статистику
        if (hits > 0 || misses > 0 || puts > 0 || queryHits > 0 || queryMisses > 0) {
            String methodName = context.getMethod().getName();
            LOGGER.info("=== L2 Cache Statistics для метода: " + methodName + " ===");
            LOGGER.info("Entity Cache Hits: " + hits);
            LOGGER.info("Entity Cache Misses: " + misses);
            LOGGER.info("Entity Cache Puts: " + puts);
            
            if (queryHits > 0 || queryMisses > 0) {
                LOGGER.info("Query Cache Hits: " + queryHits);
                LOGGER.info("Query Cache Misses: " + queryMisses);
            }
            
            // Вычисляем hit ratio
            long totalAccess = hits + misses;
            if (totalAccess > 0) {
                double hitRatio = (double) hits / totalAccess * 100;
                LOGGER.info(String.format("Cache Hit Ratio: %.2f%%", hitRatio));
            }
            
            LOGGER.info("=== Общая статистика кэша ===");
            LOGGER.info("Total Entity Cache Hits: " + hitCountAfter);
            LOGGER.info("Total Entity Cache Misses: " + missCountAfter);
            LOGGER.info("Total Entity Cache Puts: " + putCountAfter);
            
            // Выводим статистику по регионам кэша
            String[] regionNames = stats.getSecondLevelCacheRegionNames();
            if (regionNames.length > 0) {
                LOGGER.info("=== Статистика по регионам ===");
                for (String regionName : regionNames) {
                    long regionHits = stats.getCacheRegionStatistics(regionName).getHitCount();
                    long regionMisses = stats.getCacheRegionStatistics(regionName).getMissCount();
                    long regionPuts = stats.getCacheRegionStatistics(regionName).getPutCount();
                    long regionElements = stats.getCacheRegionStatistics(regionName).getElementCountInMemory();
                    
                    LOGGER.info(String.format("Region: %s | Hits: %d | Misses: %d | Puts: %d | Elements: %d",
                            regionName, regionHits, regionMisses, regionPuts, regionElements));
                }
            }
        }

        return result;
    }
}
