package org.example.models;

public enum Position {
    NATURAL("естественное"),
    UNNATURAL("неестественное");

    private final String description;

    Position(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
