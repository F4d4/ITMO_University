package org.example.models;

public class SceneObject {

    private final String name;

    public SceneObject(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя не может быть null или пустым");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
