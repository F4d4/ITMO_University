package org.example.commands;

import org.example.models.Whale;
import org.junit.jupiter.api.Test;

import static org.example.models.WhaleCondition.USUAL;
import static org.junit.jupiter.api.Assertions.*;

class CreaturePartTest {

    @Test
    void shouldContainKeyParts() {
        Whale whale = new Whale("кит", USUAL);

        CreaturePart command = new CreaturePart(whale);
        String actual = command.execute();

        assertTrue(actual.contains("несчастного существа"));
        assertTrue(actual.contains("очень мало времени"));
        assertTrue(actual.contains("оно кит"));
    }

    @Test
    void shouldThrowExceptionWhenWhaleIsNull() {
        assertThrows(NullPointerException.class, () -> new CreaturePart(null));
    }
}