package org.example.commands;

import org.example.models.Whale;

import java.util.Objects;

public class CreaturePart implements Command {
    private final Whale whale;

    public CreaturePart(Whale whale) {
        this.whale = Objects.requireNonNull(whale, "Кит не может быть null");
    }

    @Override
    public String execute() {
        return ", то у этого несчастного существа было очень мало времени на то, чтобы успеть свыкнуться с осознанием того, что оно " +
                whale.getName();
    }
}