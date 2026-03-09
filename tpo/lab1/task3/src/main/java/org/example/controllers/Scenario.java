package org.example.controllers;

import org.example.commands.Command;

import java.util.ArrayList;
import java.util.List;

public class Scenario {

    private final List<Command> commands;

    public Scenario() {
        this.commands = new ArrayList<>();
    }

    public void addCommand(Command command) {
        if (command == null) {
            throw new IllegalArgumentException("Команда не может быть null");
        }
        commands.add(command);
    }

    public int size() {
        return commands.size();
    }

    public void execute() {
        for (Command command : commands) {
            System.out.print("* ");
            command.execute();
        }
    }
}
