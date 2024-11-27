//package com.f4d4.server;
//
//import jakarta.servlet.ServletContext;
//import jakarta.servlet.http.HttpServlet;
//
//import jakarta.servlet.ServletException;
//import jakarta.servlet.annotation.WebServlet;
//import jakarta.servlet.http.HttpServlet;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//
//import javax.sql.rowset.serial.SerialBlob;
//import java.io.IOException;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.ArrayList;
//import java.util.List;
//
//public class AreaCheckServlet extends HttpServlet {
//
//    List <PointResult> results = new ArrayList<>();
//
//    @Override
//    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        ServletContext context = getServletContext();
//        try{
//            double x = Double.parseDouble(request.getParameter("x"));
//            double y = Double.parseDouble(request.getParameter("y"));
//            double r = Double.parseDouble(request.getParameter("r"));
//            long started  = System.nanoTime();
//            if(Validate.validateX(x) && Validate.validateY(y) && Validate.validateR(r)){
//                boolean hit = calculate(x,y,r);
//                long ended = System.nanoTime();
//                PointResult pointResult = new PointResult(hit, x,y,r,
//                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy ")), (ended-started)/1000);
//                results.add(pointResult);
//                Object resultsData = context.getAttribute("results");
//                ArrayList<PointResult> results = new ArrayList<PointResult>();
//                if(resultsData!=null){
//                    results.addAll((ArrayList<PointResult>)context.getAttribute("results"));
//                }
//
//
//                results.add(pointResult);
//                context.setAttribute("results", results);
//                request.setAttribute("new_result", pointResult);
//                request.getRequestDispatcher("/result.jsp").forward(request,response);
//            }else{
//                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//                return;
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//
//        request.getRequestDispatcher("/result.jsp").forward(request,response);
//
//    }
//
//    private static class Validate{
//        public static boolean validateX(double x){
//            return x>=-2&&x<=2;
//        }
//
//        public static boolean validateY(double y){
//            return y>=-3&&y<=5;
//        }
//
//        public static boolean validateR(double r){
//            return r>=-1&&r<=3;
//        }
//    }
//
//
//
//    private static boolean calculate(double x, double y, double r) {
//        if (x>=0 && y>=0 && x<=r && y<=r/2) {
//            return true;
//        }
//        if (x<=0 && y>=0 && x>=-r && y<=r/2 && 0.5*x-y +r*0.5>=0) {
//            return true;
//        }
//        if ( x<=0 && y<=0 && x>=-r && y>=-r && Math.pow(x,2)+Math.pow(y,2)<=Math.pow(r,2) ) {
//            return true;
//        }
//        return false;
//    }
//}


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

    private final List<PointResult> results = new CopyOnWriteArrayList<>();

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
            e.printStackTrace(); // Лучше заменить на запись в лог
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static class Validate {
        public static boolean validateX(double x) {
            return x >= -2 && x <= 2;
        }

        public static boolean validateY(double y) {
            return y >= -3 && y <= 5;
        }

        public static boolean validateR(double r) {
            return r > 0 && r <= 3; // Изменено на r > 0
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


