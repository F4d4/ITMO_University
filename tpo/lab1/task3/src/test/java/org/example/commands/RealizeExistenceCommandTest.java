package org.example.commands;

import org.example.models.Whale;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import static org.junit.jupiter.api.Assertions.*;

class RealizeExistenceCommandTest {

    @Test
    void testNullWhaleThrows() {
        assertThrows(NullPointerException.class, () -> new RealizeExistenceCommand(null));
    }

    @Test
    void testExecuteSetsAwarenessOfExistence() {
        Whale whale = new Whale("Кит");
        assertFalse(whale.isAwareOfExistence());

        new RealizeExistenceCommand(whale).execute();

        assertTrue(whale.isAwareOfExistence());
    }

    @Test
    void testExecuteDoesNotSetAwarenessOfDeath() {
        Whale whale = new Whale("Кит");
        new RealizeExistenceCommand(whale).execute();
        assertFalse(whale.isAwareOfDeath());
    }

    @Test
    void testExecutePrintsOutput() {
        Whale whale = new Whale("Кит");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(out);
        System.setOut(ps);

        new RealizeExistenceCommand(whale).execute();

        ps.flush();
        System.setOut(System.out);

        assertTrue(out.size() > 0, "Команда должна выводить текст");
    }
}
