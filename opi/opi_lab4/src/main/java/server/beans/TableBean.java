package server.beans;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import server.models.Point;
import server.utils.Area;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import server.data.DataBase;

@Named("tableBean")
@ApplicationScoped
public class TableBean {

    private List<Point> points = new ArrayList<>();

    public List<Point> getPoints() {
        try(var connection = DataBase.connect()){
            DataBase.createPointsTable(connection);
            points = DataBase.getLastPoints(connection);
        }catch(SQLException e ){
            e.printStackTrace();
        }
        return points;
    }



}
