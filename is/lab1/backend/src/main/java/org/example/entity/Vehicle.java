package org.example.entity;

import java.io.Serializable;
import java.util.Date;

/**
 * Транспортное средство
 * Маппинг через Vehicle.hbm.xml
 */
public class Vehicle implements Serializable {

    private Integer id; // Значение поля должно быть больше 0, уникальным, генерируется автоматически
    private String name; // Поле не может быть null, Строка не может быть пустой
    private Coordinates coordinates; // Поле не может быть null
    private Date creationDate; // Поле не может быть null, генерируется автоматически
    private VehicleType type; // Поле может быть null
    private int enginePower; // Значение поля должно быть больше 0
    private int numberOfWheels; // Значение поля должно быть больше 0
    private double capacity; // Значение поля должно быть больше 0
    private long distanceTravelled; // Значение поля должно быть больше 0
    private long fuelConsumption; // Значение поля должно быть больше 0
    private FuelType fuelType; // Поле может быть null

    public Vehicle() {
    }

    // Getters and Setters
    
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        this.name = name;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }
    
    public void setCoordinates(Coordinates coordinates) {
        if (coordinates == null) {
            throw new IllegalArgumentException("Coordinates cannot be null");
        }
        this.coordinates = coordinates;
    }

    public Date getCreationDate() {
        return creationDate;
    }
    
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
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
        if (enginePower <= 0) {
            throw new IllegalArgumentException("Engine power must be greater than 0");
        }
        this.enginePower = enginePower;
    }

    public int getNumberOfWheels() {
        return numberOfWheels;
    }
    
    public void setNumberOfWheels(int numberOfWheels) {
        if (numberOfWheels <= 0) {
            throw new IllegalArgumentException("Number of wheels must be greater than 0");
        }
        this.numberOfWheels = numberOfWheels;
    }

    public double getCapacity() {
        return capacity;
    }
    
    public void setCapacity(double capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0");
        }
        this.capacity = capacity;
    }

    public long getDistanceTravelled() {
        return distanceTravelled;
    }
    
    public void setDistanceTravelled(long distanceTravelled) {
        if (distanceTravelled < 0) {
            throw new IllegalArgumentException("Distance travelled cannot be negative");
        }
        this.distanceTravelled = distanceTravelled;
    }

    public long getFuelConsumption() {
        return fuelConsumption;
    }
    
    public void setFuelConsumption(long fuelConsumption) {
        if (fuelConsumption <= 0) {
            throw new IllegalArgumentException("Fuel consumption must be greater than 0");
        }
        this.fuelConsumption = fuelConsumption;
    }

    public FuelType getFuelType() {
        return fuelType;
    }
    
    public void setFuelType(FuelType fuelType) {
        this.fuelType = fuelType;
    }
}
