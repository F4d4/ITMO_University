package org.example.models;

import org.example.interfaces.Conscious;

import java.util.Objects;

public class Whale extends SceneObject implements Conscious {

    private Position position;
    private boolean awareOfExistence;
    private boolean awareOfDeath;

    public Whale(String name) {
        super(name);
        this.position = Position.UNNATURAL;
        this.awareOfExistence = false;
        this.awareOfDeath = false;
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
