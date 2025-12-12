package org.example.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.example.dto.CoordinatesDTO;
import org.example.dto.ErrorResponse;
import org.example.service.CoordinatesService;

import java.util.List;
import java.util.logging.Logger;

/**
 * REST контроллер для работы с Coordinates
 */
@Path("/coordinates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CoordinatesController {
    
    private static final Logger LOGGER = Logger.getLogger(CoordinatesController.class.getName());
    
    @Inject
    private CoordinatesService coordinatesService;
    
    /**
     * Получить все координаты
     * GET /api/coordinates
     */
    @GET
    public Response getAllCoordinates() {
        try {
            List<CoordinatesDTO> dtos = coordinatesService.getAllCoordinates();
            return Response.ok(dtos).build();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при получении списка Coordinates: " + e.getMessage());
            e.printStackTrace();
            ErrorResponse error = new ErrorResponse(500, "Internal Server Error",
                    "Не удалось получить список Coordinates", "/api/coordinates");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }
    
    /**
     * Получить координаты по ID
     * GET /api/coordinates/{id}
     */
    @GET
    @Path("/{id}")
    public Response getCoordinatesById(@PathParam("id") Long id) {
        try {
            CoordinatesDTO dto = coordinatesService.getCoordinatesById(id);
            return Response.ok(dto).build();
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Coordinates не найдены: " + e.getMessage());
            ErrorResponse error = new ErrorResponse(404, "Not Found", e.getMessage(),
                    "/api/coordinates/" + id);
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при получении Coordinates: " + e.getMessage());
            e.printStackTrace();
            ErrorResponse error = new ErrorResponse(500, "Internal Server Error",
                    "Не удалось получить Coordinates", "/api/coordinates/" + id);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }
}


