package com.f4d4.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;


public class ControllerServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String x = request.getParameter("x");
        String y = request.getParameter("y");
        String r = request.getParameter("r");

        // Если параметры указаны, перенаправляем на AreaCheckServlet
        if (x != null && y != null && r != null) {
            getServletContext().getRequestDispatcher("/check").forward(request, response);
        } else {
            // Иначе показываем JSP-страницу
            getServletContext().getRequestDispatcher("/index.jsp").forward(request, response);
        }
    }


}

