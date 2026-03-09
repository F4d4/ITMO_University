package org.example.commands;

import org.example.models.Whale;

import java.util.Objects;

/**
 * Команда: кит вынужден свыкнуться с осознанием того, что оно уже больше не кит.
 * «...свыкнуться с осознанием того, что оно уже больше не кит.»
 */
public class AcceptFateCommand implements Command {

    private final Whale whale;

    public AcceptFateCommand(Whale whale) {
        this.whale = Objects.requireNonNull(whale, "Кит не может быть null");
    }

    @Override
    public void execute() {
        whale.becomeAwareOfDeath();
        System.out.println(whale.getName()
                + " пришлось свыкнуться с осознанием того, что оно уже больше не кит.");
    }
}
