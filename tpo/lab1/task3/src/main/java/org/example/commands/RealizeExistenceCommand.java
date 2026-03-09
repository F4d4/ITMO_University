package org.example.commands;

import org.example.models.Whale;

import java.util.Objects;

/**
 * Команда: кит свыкается с осознанием того, что оно кит.
 * «...свыкнуться с осознанием того, что оно кит...»
 */
public class RealizeExistenceCommand implements Command {

    private final Whale whale;

    public RealizeExistenceCommand(Whale whale) {
        this.whale = Objects.requireNonNull(whale, "Кит не может быть null");
    }

    @Override
    public void execute() {
        whale.becomeAwareOfExistence();
        System.out.println("У несчастного " + whale.getName().toLowerCase()
                + " было очень мало времени, чтобы свыкнуться с осознанием того, что оно кит.");
    }
}
