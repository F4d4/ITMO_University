package org.example.commands;

import java.util.ArrayList;
import java.util.List;

public class Scenario {
    private final List<Command> commands = new ArrayList<>();

    public Scenario addCommand(Command command) {
        commands.add(command);
        return this;
    }

    public String execute() {
        StringBuilder builder = new StringBuilder();
        for (Command command : commands) {
            builder.append(command.execute());
        }
        return builder.toString();
    }
}