package com.f4d4.server;

public class PointResult {

    private boolean res;
    private double x;
    private double y;
    private double r;
    private String dateOfRequest;
    private long executionTime;


    public PointResult(boolean res, double x, double y, double r, String dateOfRequest,long executionTime) {
        this.res = res;
        this.x = x;
        this.y = y;
        this.r = r;
        this.dateOfRequest = dateOfRequest;
        this.executionTime =executionTime ;
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
}
