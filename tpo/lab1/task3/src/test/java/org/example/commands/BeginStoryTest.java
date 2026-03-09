package org.example.commands;

import org.example.models.Whale;
import org.junit.jupiter.api.Test;

import static org.example.models.WhaleCondition.UNUSUAL;
import static org.example.models.WhaleCondition.USUAL;
import static org.junit.jupiter.api.Assertions.*;

class BeginStoryTest {

    @Test
    void shouldContainKeyPartsForUsualCondition() {
        Whale whale = new Whale("кит", USUAL);

        BeginStory command = new BeginStory(whale);
        String actual = command.execute();

        assertTrue(actual.startsWith("И поскольку"));
        assertTrue(actual.contains("естественное"));
        assertTrue(actual.contains("положение"));
        assertTrue(actual.contains("кита"));
    }

    @Test
    void shouldContainKeyPartsForUnusualCondition() {
        Whale whale = new Whale("кит", UNUSUAL);

        BeginStory command = new BeginStory(whale);
        String actual = command.execute();

        assertTrue(actual.startsWith("И поскольку"));
        assertTrue(actual.contains("положение"));
        assertTrue(actual.contains("кита"));
    }

    @Test
    void shouldThrowExceptionWhenWhaleIsNull() {
        assertThrows(NullPointerException.class, () -> new BeginStory(null));
    }
}