package org.example.yourapp;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.Set;
import java.util.HashSet;

import org.example.controllers.PointController;
import org.example.controllers.UserController;
import org.example.controllers.ProtectedController;
import org.example.filters.CORSFilter;
import org.example.filters.JwtFilter;

@ApplicationPath("/api")
public class YourAppApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(UserController.class);
        classes.add(ProtectedController.class);
        classes.add(CORSFilter.class);
        classes.add(JwtFilter.class);
        classes.add(PointController.class);
        return classes;
    }
}