package org.example.services;

import org.example.entities.Point;
import org.example.entities.User;
import org.example.repositories.PointRepository;
import org.example.repositories.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;


@ApplicationScoped
public class PointService {

    @Inject
    private PointRepository pointRepository;

    @Inject
    private UserRepository userRepository;

    public Point checkAndSavePoint(Point point, String username) {
        boolean hit = checkHit(point.getX(), point.getY(), point.getR());
        point.setHit(hit);
        point.setTime(System.currentTimeMillis());

        User user = userRepository.findByUsername(username);
        point.setUser(user);
        pointRepository.createPoint(point);
        return point;
    }

    private boolean checkHit(double x, double y, double r) {


        if (x >= 0 && y >= 0) {
            return (y<= -x + r/2);
        } else if (x >= 0 && y <= 0) {
            return (x<=r/2 && y >= -r );
        } else if (x <= 0 && y <= 0) {
            return ((x*x + y*y) <= (r/2)*(r/2));
        }
        return false;
    }

    public List<Point> getPointsByUser(String username) {
        User user = userRepository.findByUsername(username);
        return pointRepository.findPointsByUser(user);
    }
}
