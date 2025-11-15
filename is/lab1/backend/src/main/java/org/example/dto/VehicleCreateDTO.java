package org.example.dto;

import jakarta.validation.constraints.*;
import org.example.entity.FuelType;
import org.example.entity.VehicleType;

import java.io.Serializable;

/**
 * DTO для создания нового Vehicle
 */
public class VehicleCreateDTO implements Serializable {
    
    @NotBlank(message = "Имя не может быть пустым")
    private String name;
    
    private double x;
    
    @Max(value = 621, message = "Y координата не может превышать 621")
    private long y;
    
    private VehicleType type;
    
    @Positive(message = "Мощность двигателя должна быть больше 0")
    private int enginePower;
    
    @Positive(message = "Количество колес должно быть больше 0")
    private int numberOfWheels;
    
    @Positive(message = "Вместимость должна быть больше 0")
    private double capacity;
    
    @PositiveOrZero(message = "Пройденное расстояние не может быть отрицательным")
    private long distanceTravelled;
    
    @Positive(message = "Расход топлива должен быть больше 0")
    private long fuelConsumption;
    
    private FuelType fuelType;
    
    public VehicleCreateDTO() {}

    // Getters and Setters
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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




