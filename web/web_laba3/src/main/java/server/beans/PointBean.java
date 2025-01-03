package server.beans;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import server.models.Point;
import server.utils.Area;

import java.util.ArrayList;
import java.util.List;

@Named("pointBean")
@ApplicationScoped
public class PointBean {

    private double x;
    private double y;
    private double r;

    private List<Point> points = new ArrayList<>();

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getR() {
        return r;
    }

    public void setR(double r) {
        this.r = r;
    }

    public List<Point> getPoints(){
        return points;
    }

    public void addPoint(){
        points.add(new Point(Area.Check.calculate(x,y,r), x,y,r));
    }



}
