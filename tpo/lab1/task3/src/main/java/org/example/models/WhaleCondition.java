package org.example.models;

public enum WhaleCondition {
    USUAL,
    UNUSUAL;

    @Override
    public String toString() {
        switch (this) {
            case USUAL:
                return "естественное";
            case UNUSUAL:
                return "неестественное";
            default:
                return "без состояния";
        }
    }
}