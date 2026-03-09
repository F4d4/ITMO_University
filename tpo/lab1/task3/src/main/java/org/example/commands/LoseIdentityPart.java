package org.example.commands;

import org.example.models.Whale;

import java.util.Objects;

public class LoseIdentityPart implements Command {
    private final Whale whale;

    public LoseIdentityPart(Whale whale) {
        this.whale = Objects.requireNonNull(whale, "Кит не может быть null");
    }

    @Override
    public String execute() {
        return ", перед тем, как ему пришлось свыкнуться с осознанием того, что оно уже больше не " +
                whale.getName();
    }
}