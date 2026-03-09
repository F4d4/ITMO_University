package org.example.commands;

import org.example.models.Whale;
import org.junit.jupiter.api.Test;

import static org.example.models.WhaleCondition.USUAL;
import static org.junit.jupiter.api.Assertions.*;

class LoseIdentityPartTest {

    @Test
    void shouldContainKeyParts() {
        Whale whale = new Whale("кит", USUAL);

        LoseIdentityPart command = new LoseIdentityPart(whale);
        String actual = command.execute();

        assertTrue(actual.contains("перед тем"));
        assertTrue(actual.contains("уже больше не кит"));
    }

    @Test
    void shouldThrowExceptionWhenWhaleIsNull() {
        assertThrows(NullPointerException.class, () -> new LoseIdentityPart(null));
    }
}