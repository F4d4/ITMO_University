package org.example.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextPartTest {

    @Test
    void shouldReturnTextAsIs() {
        TextPart command = new TextPart("тестовый текст");
        assertEquals("тестовый текст", command.execute());
    }

    @Test
    void shouldReturnEmptyString() {
        TextPart command = new TextPart("");
        assertEquals("", command.execute());
    }
}