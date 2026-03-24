package org.example.models;

import org.example.interfaces.Conscious;

import java.util.Objects;

public class Whale extends SceneObject implements Conscious {

    private Position position;
    private boolean awareOfExistence;
    private boolean awareOfDeath;
    private double totalTime;
    private double timeSpent;

    public Whale(String name) {
        super(name);
        this.position = Position.UNNATURAL;
        this.awareOfExistence = false;
        this.awareOfDeath = false;
        this.totalTime = 0.0;
        this.timeSpent = 0.0;
    }

    public void setTotalTime(double totalTime) {
        if (totalTime < 0) {
            throw new IllegalArgumentException("Время не может быть отрицательным");
        }
        this.totalTime = totalTime;
    }

    public double getRemainingTime() {
        return totalTime - timeSpent;
    }

    public boolean hasEnoughTime(double required) {
        return getRemainingTime() >= required;
    }

    public void spendTime(double time) {
        if (time < 0) {
            throw new IllegalArgumentException("Время не может быть отрицательным");
        }
        timeSpent += time;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = Objects.requireNonNull(position, "Положение не может быть null");
    }

    public boolean isInNaturalPosition() {
        return position == Position.NATURAL;
    }

    @Override
    public void becomeAwareOfExistence() {
        this.awareOfExistence = true;
    }

    @Override
    public void becomeAwareOfDeath() {
        this.awareOfDeath = true;
    }

    @Override
    public boolean isAwareOfExistence() {
        return awareOfExistence;
    }

    @Override
    public boolean isAwareOfDeath() {
        return awareOfDeath;
    }
}
