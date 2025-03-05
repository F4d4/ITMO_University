package org.example.controllers;

import org.example.entities.Point;
import org.example.responses.ErrorResponse;
import org.example.services.PointService;
import org.example.security.JwtUtil;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/points")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PointController {

    @Inject
    private PointService pointService;

    @Inject
    private JwtUtil jwtUtil;


    @POST
    public Response addPoint(Point point, @HeaderParam("Authorization") String authHeader) {
        String username = getUsernameFromToken(authHeader);
        if (username == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse("Unauthorized"))
                    .build();
        }
        try {
            Point result = pointService.checkAndSavePoint(point, username);
            return Response.ok(result).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Internal Server Error"))
                    .build();
        }
    }


    @GET
    public Response getPoints(@HeaderParam("Authorization") String authHeader) {
        String username = getUsernameFromToken(authHeader);
        if (username == null) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Unauthorized");
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(errorResponse)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        try {
            // Получаем список точек пользователя
            List<Point> points = pointService.getPointsByUser(username);
            // Возвращаем список точек с указанием медиатипа
            return Response.ok(points)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Exception e) {
            e.printStackTrace(); // Логирование исключения
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponse)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    private String getUsernameFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring("Bearer ".length()).trim();
        try {
            return jwtUtil.validateToken(token);
        } catch (Exception e) {
            return null;
        }
    }
}

