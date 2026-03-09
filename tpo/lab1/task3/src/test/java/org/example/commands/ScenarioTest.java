package org.example.commands;

import org.example.models.Whale;
import org.junit.jupiter.api.Test;

import static org.example.models.WhaleCondition.USUAL;
import static org.junit.jupiter.api.Assertions.*;

class ScenarioTest {

    @Test
    void shouldBuildStoryText() {
        Whale whale = new Whale("кит", USUAL);

        String actual = new Scenario()
                .addCommand(new BeginStory(whale))
                .addCommand(new CreaturePart(whale))
                .addCommand(new TextPart(", перед тем, как ему пришлось свыкнуться с осознанием того, что оно уже больше не кит."))
                .execute();

        assertTrue(actual.startsWith("И поскольку"));
        assertTrue(actual.contains("несчастного существа"));
        assertTrue(actual.contains("оно кит"));
        assertTrue(actual.contains("уже больше не кит"));
    }

    @Test
    void shouldReturnEmptyStringWhenScenarioHasNoCommands() {
        Scenario scenario = new Scenario();
        assertEquals("", scenario.execute());
    }

    @Test
    void shouldExecuteCommandsInOrder() {
        String actual = new Scenario()
                .addCommand(new TextPart("1"))
                .addCommand(new TextPart("2"))
                .addCommand(new TextPart("3"))
                .execute();

        assertEquals("123", actual);
    }
}