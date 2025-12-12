package org.example.dto;

import java.io.Serializable;

/**
 * DTO для отображения Coordinates
 */
public class CoordinatesDTO implements Serializable {
    
    private Long id;
    private double x;
    private long y;
    
    public CoordinatesDTO() {}
    
    public CoordinatesDTO(Long id, double x, long y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public double getX() {
        return x;
    }
    
    public void setX(double x) {
        this.x = x;
    }
    
    public long getY() {
        return y;
    }
    
    public void setY(long y) {
        this.y = y;
    }
}






