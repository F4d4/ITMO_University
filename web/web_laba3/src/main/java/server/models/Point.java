package server.models;

public class Point {

    private boolean res;
    private double x;
    private double y;
    private double r;


    public Point(boolean res, double x, double y, double r) {
        this.res = res;
        this.x = x;
        this.y = y;
        this.r = r;

    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getR() {
        return r;
    }

    public boolean getRes() {
        return res;
    }



    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setR(double r) {
        this.r = r;
    }

    public void setRes(boolean res){
        this.res = res;
    }



}
