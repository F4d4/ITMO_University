package org.example.commands;

import org.example.models.Whale;

import java.util.Objects;

import static org.example.models.WhaleCondition.USUAL;

public class BeginStory implements Command {
    private final Whale whale;

    public BeginStory(Whale whale) {
        this.whale = Objects.requireNonNull(whale, "Кит не может быть null");
    }

    @Override
    public String execute() {
        if (whale.getCondition() == USUAL) {
            return "И поскольку это далеко не самое " + whale.getCondition() +
                    " положение для " + whale.getName() + "а";
        } else {
            return "И поскольку это довольно " + whale.getCondition() +
                    " положение для " + whale.getName() + "а";
        }
    }
}