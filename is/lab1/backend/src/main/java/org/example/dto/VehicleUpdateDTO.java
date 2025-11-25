package org.example.dto;

import jakarta.validation.constraints.*;
import org.example.entity.FuelType;
import org.example.entity.VehicleType;

import java.io.Serializable;

/**
 * DTO для обновления существующего Vehicle
 * Все поля опциональные
 */
public class VehicleUpdateDTO implements Serializable {

    private String name;

    private Double x;

    @Max(value = 621, message = "Y координата не может превышать 621")
    private Long y;

    private VehicleType type;

    @Positive(message = "Мощность двигателя должна быть больше 0")
    private Integer enginePower;

    @Positive(message = "Количество колес должно быть больше 0")
    private Integer numberOfWheels;

    @Positive(message = "Вместимость должна быть больше 0")
    private Double capacity;

    @PositiveOrZero(message = "Пройденное расстояние не может быть отрицательным")
    private Long distanceTravelled;

    @Positive(message = "Расход топлива должен быть больше 0")
    private Long fuelConsumption;

    private FuelType fuelType;

    public VehicleUpdateDTO() {
    }

    // Getters and Setters

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

    public Integer getEnginePower() {
        return enginePower;
    }

    public void setEnginePower(Integer enginePower) {
        this.enginePower = enginePower;
    }

    public Integer getNumberOfWheels() {
        return numberOfWheels;
    }

    public void setNumberOfWheels(Integer numberOfWheels) {
        this.numberOfWheels = numberOfWheels;
    }

    public Double getCapacity() {
        return capacity;
    }

    public void setCapacity(Double capacity) {
        this.capacity = capacity;
    }

    public Long getDistanceTravelled() {
        return distanceTravelled;
    }

    public void setDistanceTravelled(Long distanceTravelled) {
        this.distanceTravelled = distanceTravelled;
    }

    public Long getFuelConsumption() {
        return fuelConsumption;
    }

    public void setFuelConsumption(Long fuelConsumption) {
        this.fuelConsumption = fuelConsumption;
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public void setFuelType(FuelType fuelType) {
        this.fuelType = fuelType;
    }
}







