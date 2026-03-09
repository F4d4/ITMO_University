package org.example.commands;

public class TextPart implements Command {
    private final String text;

    public TextPart(String text) {
        this.text = text;
    }

    @Override
    public String execute() {
        return text;
    }
}