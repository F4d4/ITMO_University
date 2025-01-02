package com.f4d4.server;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AreaCheckServlet extends HttpServlet {

    private final List<PointResult> results = new ArrayList<>();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletContext context = getServletContext();
        try {
            double x = Double.parseDouble(request.getParameter("x"));
            double y = Double.parseDouble(request.getParameter("y"));
            double r = Double.parseDouble(request.getParameter("r"));

            if (!Validate.validateX(x) || !Validate.validateY(y) || !Validate.validateR(r)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            long started = System.nanoTime();
            boolean hit = calculate(x, y, r);
            long ended = System.nanoTime();

            PointResult pointResult = new PointResult(hit, x, y, r,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy")),
                    (ended - started) / 1000);

            results.add(pointResult);

            synchronized (context) {
                List<PointResult> sharedResults = (List<PointResult>) context.getAttribute("results");
                if (sharedResults == null) {
                    sharedResults = new ArrayList<>();
                    context.setAttribute("results", sharedResults);
                }
                sharedResults.add(pointResult);
            }

            request.setAttribute("new_result", pointResult);
            request.getRequestDispatcher("/result.jsp").forward(request, response);

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Некорректный формат чисел.");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static class Validate {
        public static boolean validateX(double x) {
            return x >= -5 && x <= 5;
        }

        public static boolean validateY(double y) {
            return y >= -5 && y <= 5;
        }

        public static boolean validateR(double r) {
            return r >0 && r <= 5;
        }
    }

    private static boolean calculate(double x, double y, double r) {
        if (x >= 0 && y >= 0 && x <= r && y <= r / 2) {
            return true;
        }
        if (x <= 0 && y >= 0 && x >= -r && y <= r / 2 && (0.5 * x - y + r * 0.5) >= 0) {
            return true;
        }
        return x <= 0 && y <= 0 && x >= -r && y >= -r && (Math.pow(x, 2) + Math.pow(y, 2) <= Math.pow(r, 2));
    }
}


