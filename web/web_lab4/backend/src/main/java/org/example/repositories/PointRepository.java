package org.example.repositories;

import org.example.entities.Point;
import org.example.entities.User;
import java.util.List;

public interface PointRepository {
    void createPoint(Point point);
    List<Point> findPointsByUser(User user);
}
