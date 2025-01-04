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

    private Double x;
    private Double y;
    private Double r;

    private List<Point> points = new ArrayList<>();

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public Double getR() {
        return r;
    }

    public void setR(Double r) {
        this.r = r;
    }

    public List<Point> getPoints() {
        return points;
    }

    public void addPoint() {
        points.add(new Point(Area.Check.calculate(x, y, r), x, y, r));
    }

    public boolean isValid() {
        if (y == null ||y < -3 || y > 3){
            return false;
        }

        double[] validXvalues = { -5 , -4 , -3 , -2 , -1 , 0 , 1 , 2 , 3};
        boolean xValid = false;
        for(double validValue : validXvalues){
            if(x == validValue){
                xValid = true;
                break;
            }
        }

        if(!xValid||x==null){
            return false;
        }

        double[] validRvalues = { -5 , -4 , -3 , -2 , -1 , 0 , 1 , 2 , 3};
        boolean rValid = false;
        for(double validValue : validXvalues){
            if(x == validValue){
                rValid = true;
                break;
            }
        }

        if(!rValid || r==null){
            return false;
        }

        return true;


    }


}
