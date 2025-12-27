package org.example.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.example.service.CacheStatisticsService;

import java.util.Map;
import java.util.logging.Logger;

/**
 * REST контроллер для управления и мониторинга L2 Cache
 */
@Path("/cache")
@Produces(MediaType.APPLICATION_JSON)
public class CacheController {

    private static final Logger LOGGER = Logger.getLogger(CacheController.class.getName());

    @Inject
    private CacheStatisticsService cacheStatisticsService;

    /**
     * Получить статистику кэша
     * GET /api/cache/statistics
     */
    @GET
    @Path("/statistics")
    public Response getStatistics() {
        try {
            Map<String, Object> stats = cacheStatisticsService.getCacheStatistics();
            return Response.ok(stats).build();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при получении статистики кэша: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    /**
     * Сбросить статистику кэша
     * POST /api/cache/statistics/clear
     */
    @POST
    @Path("/statistics/clear")
    public Response clearStatistics() {
        try {
            cacheStatisticsService.clearStatistics();
            return Response.ok(Map.of("message", "Статистика кэша успешно сброшена")).build();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при сбросе статистики кэша: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    /**
     * Очистить все кэши
     * POST /api/cache/evict/all
     */
    @POST
    @Path("/evict/all")
    public Response evictAll() {
        try {
            cacheStatisticsService.evictAllCaches();
            return Response.ok(Map.of("message", "Все кэши успешно очищены")).build();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при очистке кэшей: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    /**
     * Очистить кэш конкретной сущности
     * POST /api/cache/evict/{entityName}
     */
    @POST
    @Path("/evict/{entityName}")
    public Response evictEntity(@PathParam("entityName") String entityName) {
        try {
            cacheStatisticsService.evictEntityCache(entityName);
            return Response.ok(Map.of("message", "Кэш сущности " + entityName + " успешно очищен")).build();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при очистке кэша сущности: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    /**
     * Включить статистику
     * POST /api/cache/statistics/enable
     */
    @POST
    @Path("/statistics/enable")
    public Response enableStatistics() {
        try {
            cacheStatisticsService.setStatisticsEnabled(true);
            return Response.ok(Map.of("message", "Статистика кэша включена")).build();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при включении статистики: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    /**
     * Отключить статистику
     * POST /api/cache/statistics/disable
     */
    @POST
    @Path("/statistics/disable")
    public Response disableStatistics() {
        try {
            cacheStatisticsService.setStatisticsEnabled(false);
            return Response.ok(Map.of("message", "Статистика кэша отключена")).build();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при отключении статистики: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
}
