package org.example.commands;

import org.example.models.Whale;

import java.util.Objects;

/**
 * Команда: описание неестественного положения, в котором оказался кит.
 * «...это далеко не самое естественное положение для кита...»
 */
public class DescribePositionCommand implements Command {

    private final Whale whale;

    public DescribePositionCommand(Whale whale) {
        this.whale = Objects.requireNonNull(whale, "Кит не может быть null");
    }

    @Override
    public void execute() {
        System.out.println("Это далеко не самое естественное положение для "
                + whale.getName().toLowerCase() + ".");
    }
}
