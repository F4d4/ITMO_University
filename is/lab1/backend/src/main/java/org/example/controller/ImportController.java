package org.example.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.example.dto.*;
import org.example.service.ImportService;

import java.util.List;
import java.util.logging.Logger;

/**
 * REST контроллер для операций импорта
 */
@Path("/import")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ImportController {

    private static final Logger LOGGER = Logger.getLogger(ImportController.class.getName());

    @Inject
    private ImportService importService;

    /**
     * Импорт Vehicle из JSON
     * POST /api/import/vehicles
     * 
     * Заголовки:
     * - X-Username: имя пользователя
     * - X-Is-Admin: true/false - является ли пользователь администратором
     * 
     * Body: JSON массив объектов VehicleImportDTO
     */
    @POST
    @Path("/vehicles")
    public Response importVehicles(
            @HeaderParam("X-Username") @DefaultValue("user") String username,
            @HeaderParam("X-Is-Admin") @DefaultValue("false") boolean isAdmin,
            List<VehicleImportDTO> vehicles) {
        
        try {
            LOGGER.info("Получен запрос на импорт от пользователя: " + username + " (admin=" + isAdmin + ")");
            
            if (vehicles == null || vehicles.isEmpty()) {
                ErrorResponse error = new ErrorResponse(400, "Bad Request",
                        "Файл не содержит данных для импорта", "/api/import/vehicles");
                return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
            }
            
            LOGGER.info("Получено " + vehicles.size() + " объектов для импорта");
            
            // Выполняем импорт
            ImportResultDTO result = importService.importVehicles(vehicles, username, isAdmin);
            
            if ("SUCCESS".equals(result.getStatus())) {
                return Response.status(Response.Status.CREATED).entity(result).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity(result).build();
            }
            
        } catch (Exception e) {
            LOGGER.severe("Ошибка при импорте: " + e.getMessage());
            e.printStackTrace();
            ErrorResponse error = new ErrorResponse(500, "Internal Server Error",
                    "Ошибка при импорте: " + e.getMessage(), "/api/import/vehicles");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }

    /**
     * Получить историю импорта
     * GET /api/import/history
     * 
     * Заголовки:
     * - X-Username: имя пользователя
     * - X-Is-Admin: true/false - является ли пользователь администратором
     */
    @GET
    @Path("/history")
    public Response getImportHistory(
            @HeaderParam("X-Username") @DefaultValue("user") String username,
            @HeaderParam("X-Is-Admin") @DefaultValue("false") boolean isAdmin) {
        
        try {
            LOGGER.info("Запрос истории импорта от пользователя: " + username + " (admin=" + isAdmin + ")");
            
            List<ImportOperationDTO> history = importService.getImportHistory(username, isAdmin);
            
            return Response.ok(history).build();
            
        } catch (Exception e) {
            LOGGER.severe("Ошибка при получении истории импорта: " + e.getMessage());
            ErrorResponse error = new ErrorResponse(500, "Internal Server Error",
                    "Ошибка при получении истории: " + e.getMessage(), "/api/import/history");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }
}
