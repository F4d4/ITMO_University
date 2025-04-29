package server.models;

public class Point {

    private boolean res;
    private double x;
    private double y;
    private double r;

    private long executionTime;

    private String dateOfRequest;


    public Point(boolean res, double x, double y, double r , String dateOfRequest, long executionTime) {
        this.res = res;
        this.x = x;
        this.y = y;
        this.r = r;
        this.dateOfRequest = dateOfRequest;
        this.executionTime = executionTime;

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

    public String getDateOfRequest(){
        return dateOfRequest;
    }

    public long getExecutionTime() {
        return executionTime;
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

    public void setDateOfRequest(String dateOfRequest) {
        this.dateOfRequest = dateOfRequest;
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }
}
