package org.example.models;

import java.util.Objects;

public abstract class BaseModel {
    private final String name;

    public BaseModel(String name) {
        this.name = Objects.requireNonNull(name, "Имя не может быть null");
    }

    public String getName() {
        return name;
    }
}