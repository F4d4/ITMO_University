package org.example.entity;

import java.io.Serializable;

/**
 * Координаты транспортного средства
 * Маппинг через Coordinates.hbm.xml
 */
public class Coordinates implements Serializable {
    
    private Long id;
    private double x;
    private long y; // Максимальное значение поля: 621
    
    public Coordinates() {}
    
    public Coordinates(double x, long y) {
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
        if (y > 621) {
            throw new IllegalArgumentException("Y value cannot exceed 621");
        }
        this.y = y; 
    }
}
