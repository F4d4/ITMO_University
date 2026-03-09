package org.example.commands;

import org.example.models.Whale;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import static org.junit.jupiter.api.Assertions.*;

class AcceptFateCommandTest {

    @Test
    void testNullWhaleThrows() {
        assertThrows(NullPointerException.class, () -> new AcceptFateCommand(null));
    }

    @Test
    void testExecuteSetsAwarenessOfDeath() {
        Whale whale = new Whale("Кит");
        assertFalse(whale.isAwareOfDeath());

        new AcceptFateCommand(whale).execute();

        assertTrue(whale.isAwareOfDeath());
    }

    @Test
    void testExecuteDoesNotAffectExistenceAwareness() {
        Whale whale = new Whale("Кит");
        new AcceptFateCommand(whale).execute();
        assertFalse(whale.isAwareOfExistence());
    }

    @Test
    void testExecutePrintsOutput() {
        Whale whale = new Whale("Кит");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(out);
        System.setOut(ps);

        new AcceptFateCommand(whale).execute();

        ps.flush();
        System.setOut(System.out);

        assertTrue(out.size() > 0, "Команда должна выводить текст");
    }
}
