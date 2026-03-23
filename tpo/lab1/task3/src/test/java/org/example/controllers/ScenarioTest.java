package org.example.controllers;

import org.example.commands.AcceptFateCommand;
import org.example.commands.DescribePositionCommand;
import org.example.commands.RealizeExistenceCommand;
import org.example.models.Whale;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import static org.junit.jupiter.api.Assertions.*;

class ScenarioTest {

    @Test
    void testEmptyScenarioExecutesWithoutError() {
        Scenario scenario = new Scenario();
        assertDoesNotThrow(scenario::execute);
    }

    @Test
    void testAddNullCommandThrows() {
        Scenario scenario = new Scenario();
        assertThrows(IllegalArgumentException.class, () -> scenario.addCommand(null));
    }

    @Test
    void testScenarioSizeAfterAddingCommands() {
        Whale whale = new Whale("Кит");
        Scenario scenario = new Scenario();
        scenario.addCommand(new DescribePositionCommand(whale));
        scenario.addCommand(new RealizeExistenceCommand(whale));
        scenario.addCommand(new AcceptFateCommand(whale));

        assertEquals(3, scenario.size());
    }

    @Test
    void testScenarioExecutesAllCommands() {
        Whale whale = new Whale("Кит");
        Scenario scenario = new Scenario();
        scenario.addCommand(new DescribePositionCommand(whale));
        scenario.addCommand(new RealizeExistenceCommand(whale));
        scenario.addCommand(new AcceptFateCommand(whale));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        scenario.execute();

        System.setOut(System.out);
        String output = out.toString();

        long lineCount = output.lines().filter(l -> l.startsWith("*")).count();
        assertEquals(3, lineCount, "Сценарий должен выполнить 3 команды");
    }

    @Test
    void testScenarioSetsWhaleStateAfterExecution() {
        Whale whale = new Whale("Кит");
        Scenario scenario = new Scenario();
        scenario.addCommand(new RealizeExistenceCommand(whale));
        scenario.addCommand(new AcceptFateCommand(whale));

        scenario.execute();

        assertAll(
                () -> assertTrue(whale.isAwareOfExistence()),
                () -> assertTrue(whale.isAwareOfDeath())
        );
    }
}
