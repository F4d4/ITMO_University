package org.example.dto;

import org.example.entity.FuelType;
import org.example.entity.VehicleType;

import java.io.Serializable;

/**
 * DTO для импорта Vehicle из JSON
 * Содержит все поля включая вложенные координаты
 */
public class VehicleImportDTO implements Serializable {
    
    private String name;
    
    // Вложенные координаты
    private Double x;
    private Long y;
    
    private VehicleType type;
    private int enginePower;
    private int numberOfWheels;
    private double capacity;
    private long distanceTravelled;
    private long fuelConsumption;
    private FuelType fuelType;

    public VehicleImportDTO() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Long getY() {
        return y;
    }

    public void setY(Long y) {
        this.y = y;
    }

    public VehicleType getType() {
        return type;
    }

    public void setType(VehicleType type) {
        this.type = type;
    }

    public int getEnginePower() {
        return enginePower;
    }

    public void setEnginePower(int enginePower) {
        this.enginePower = enginePower;
    }

    public int getNumberOfWheels() {
        return numberOfWheels;
    }

    public void setNumberOfWheels(int numberOfWheels) {
        this.numberOfWheels = numberOfWheels;
    }

    public double getCapacity() {
        return capacity;
    }

    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }

    public long getDistanceTravelled() {
        return distanceTravelled;
    }

    public void setDistanceTravelled(long distanceTravelled) {
        this.distanceTravelled = distanceTravelled;
    }

    public long getFuelConsumption() {
        return fuelConsumption;
    }

    public void setFuelConsumption(long fuelConsumption) {
        this.fuelConsumption = fuelConsumption;
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public void setFuelType(FuelType fuelType) {
        this.fuelType = fuelType;
    }
}

