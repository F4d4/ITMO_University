package org.example.controller;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.example.dto.*;
import org.example.entity.VehicleType;
import org.example.service.VehicleService;

import java.util.List;
import java.util.logging.Logger;

/**
 * REST контроллер для работы с Vehicle
 */
@Path("/vehicles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VehicleController {

    private static final Logger LOGGER = Logger.getLogger(VehicleController.class.getName());

    @Inject
    private VehicleService vehicleService;

    /**
     * Создать новый Vehicle
     * POST /api/vehicles
     */
    @POST
    public Response createVehicle(@Valid VehicleCreateDTO createDTO) {
        try {
            VehicleDTO created = vehicleService.createVehicle(createDTO);
            return Response.status(Response.Status.CREATED).entity(created).build();
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Ошибка валидации при создании Vehicle: " + e.getMessage());
            ErrorResponse error = new ErrorResponse(400, "Bad Request", e.getMessage(), "/api/vehicles");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при создании Vehicle: " + e.getMessage());
            ErrorResponse error = new ErrorResponse(500, "Internal Server Error",
                    "Не удалось создать Vehicle", "/api/vehicles");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }

    /**
     * Получить Vehicle по ID
     * GET /api/vehicles/{id}
     */
    @GET
    @Path("/{id}")
    public Response getVehicleById(@PathParam("id") Integer id) {
        try {
            VehicleDTO vehicle = vehicleService.getVehicleById(id);
            return Response.ok(vehicle).build();
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Vehicle не найден: " + e.getMessage());
            ErrorResponse error = new ErrorResponse(404, "Not Found", e.getMessage(),
                    "/api/vehicles/" + id);
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при получении Vehicle: " + e.getMessage());
            ErrorResponse error = new ErrorResponse(500, "Internal Server Error",
                    "Не удалось получить Vehicle", "/api/vehicles/" + id);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }

    /**
     * Получить все Vehicle с пагинацией и фильтрацией
     * GET
     * /api/vehicles?page=0&size=10&filterField=name&filterValue=car&sortField=name&sortDirection=asc
     */
    @GET
    public Response getAllVehicles(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("filterField") String filterField,
            @QueryParam("filterValue") String filterValue,
            @QueryParam("sortField") String sortField,
            @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection) {
        try {
            PaginatedResponse<VehicleDTO> response;

            if (filterField != null && !filterField.isEmpty()) {
                response = vehicleService.getVehiclesWithFilters(
                        filterField, filterValue, sortField, sortDirection, page, size);
            } else {
                response = vehicleService.getAllVehicles(page, size);
            }

            return Response.ok(response).build();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при получении списка Vehicle: " + e.getMessage());
            ErrorResponse error = new ErrorResponse(500, "Internal Server Error",
                    "Не удалось получить список Vehicle", "/api/vehicles");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }

    /**
     * Обновить Vehicle
     * PUT /api/vehicles/{id}
     */
    @PUT
    @Path("/{id}")
    public Response updateVehicle(@PathParam("id") Integer id, @Valid VehicleUpdateDTO updateDTO) {
        try {
            VehicleDTO updated = vehicleService.updateVehicle(id, updateDTO);
            return Response.ok(updated).build();
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Ошибка при обновлении Vehicle: " + e.getMessage());
            ErrorResponse error = new ErrorResponse(400, "Bad Request", e.getMessage(),
                    "/api/vehicles/" + id);
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при обновлении Vehicle: " + e.getMessage());
            ErrorResponse error = new ErrorResponse(500, "Internal Server Error",
                    "Не удалось обновить Vehicle", "/api/vehicles/" + id);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }

    /**
     * Удалить Vehicle
     * DELETE /api/vehicles/{id}
     */
    @DELETE
    @Path("/{id}")
    public Response deleteVehicle(@PathParam("id") Integer id) {
        try {
            vehicleService.deleteVehicle(id);
            return Response.noContent().build();
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Vehicle не найден для удаления: " + e.getMessage());
            ErrorResponse error = new ErrorResponse(404, "Not Found", e.getMessage(),
                    "/api/vehicles/" + id);
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при удалении Vehicle: " + e.getMessage());
            ErrorResponse error = new ErrorResponse(500, "Internal Server Error",
                    "Не удалось удалить Vehicle", "/api/vehicles/" + id);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }

    /**
     * Получить Vehicle с максимальной capacity
     * GET /api/vehicles/max-capacity
     */
    @GET
    @Path("/max-capacity")
    public Response getVehicleWithMaxCapacity() {
        try {
            VehicleDTO vehicle = vehicleService.getVehicleWithMaxCapacity();
            return Response.ok(vehicle).build();
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Vehicle не найден: " + e.getMessage());
            ErrorResponse error = new ErrorResponse(404, "Not Found", e.getMessage(),
                    "/api/vehicles/max-capacity");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при получении Vehicle с максимальной capacity: " + e.getMessage());
            ErrorResponse error = new ErrorResponse(500, "Internal Server Error",
                    "Не удалось получить Vehicle", "/api/vehicles/max-capacity");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }

    /**
     * Получить Vehicle по префиксу имени
     * GET /api/vehicles/by-name-prefix?prefix=Car
     */
    @GET
    @Path("/by-name-prefix")
    public Response getVehiclesByNamePrefix(@QueryParam("prefix") String prefix) {
        try {
            if (prefix == null || prefix.trim().isEmpty()) {
                ErrorResponse error = new ErrorResponse(400, "Bad Request",
                        "Параметр 'prefix' обязателен", "/api/vehicles/by-name-prefix");
                return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
            }

            List<VehicleDTO> vehicles = vehicleService.getVehiclesByNamePrefix(prefix);
            return Response.ok(vehicles).build();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при поиске Vehicle по префиксу: " + e.getMessage());
            ErrorResponse error = new ErrorResponse(500, "Internal Server Error",
                    "Не удалось найти Vehicle", "/api/vehicles/by-name-prefix");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }

    /**
     * Получить Vehicle с расходом топлива больше заданного
     * GET /api/vehicles/by-fuel-consumption?minConsumption=100
     */
    @GET
    @Path("/by-fuel-consumption")
    public Response getVehiclesByFuelConsumption(@QueryParam("minConsumption") long minConsumption) {
        try {
            List<VehicleDTO> vehicles = vehicleService.getVehiclesByFuelConsumption(minConsumption);
            return Response.ok(vehicles).build();
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Ошибка валидации: " + e.getMessage());
            ErrorResponse error = new ErrorResponse(400, "Bad Request", e.getMessage(),
                    "/api/vehicles/by-fuel-consumption");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при поиске Vehicle по расходу топлива: " + e.getMessage());
            ErrorResponse error = new ErrorResponse(500, "Internal Server Error",
                    "Не удалось найти Vehicle", "/api/vehicles/by-fuel-consumption");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }

    /**
     * Получить Vehicle по типу
     * GET /api/vehicles/by-type?type=CAR
     */
    @GET
    @Path("/by-type")
    public Response getVehiclesByType(@QueryParam("type") VehicleType type) {
        try {
            if (type == null) {
                ErrorResponse error = new ErrorResponse(400, "Bad Request",
                        "Параметр 'type' обязателен", "/api/vehicles/by-type");
                return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
            }

            List<VehicleDTO> vehicles = vehicleService.getVehiclesByType(type);
            return Response.ok(vehicles).build();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при поиске Vehicle по типу: " + e.getMessage());
            ErrorResponse error = new ErrorResponse(500, "Internal Server Error",
                    "Не удалось найти Vehicle", "/api/vehicles/by-type");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }

    /**
     * Скрутить счётчик пробега до нуля
     * POST /api/vehicles/{id}/reset-distance
     */
    @POST
    @Path("/{id}/reset-distance")
    public Response resetDistanceTravelled(@PathParam("id") Integer id) {
        try {
            vehicleService.resetDistanceTravelled(id);
            return Response.ok().build();
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Vehicle не найден: " + e.getMessage());
            ErrorResponse error = new ErrorResponse(404, "Not Found", e.getMessage(),
                    "/api/vehicles/" + id + "/reset-distance");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при сбросе пробега: " + e.getMessage());
            ErrorResponse error = new ErrorResponse(500, "Internal Server Error",
                    "Не удалось сбросить пробег", "/api/vehicles/" + id + "/reset-distance");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }
}







