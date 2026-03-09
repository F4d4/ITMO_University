package org.example.commands;

import org.example.models.Whale;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import static org.junit.jupiter.api.Assertions.*;

class DescribePositionCommandTest {

    @Test
    void testNullWhaleThrows() {
        assertThrows(NullPointerException.class, () -> new DescribePositionCommand(null));
    }

    @Test
    void testExecutePrintsPositionDescription() {
        Whale whale = new Whale("Кит");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(out);
        System.setOut(ps);

        new DescribePositionCommand(whale).execute();

        ps.flush();
        System.setOut(System.out);

        assertTrue(out.size() > 0, "Команда должна выводить текст");
    }

    @Test
    void testExecuteDoesNotChangeWhaleState() {
        Whale whale = new Whale("Кит");
        new DescribePositionCommand(whale).execute();

        assertFalse(whale.isAwareOfExistence());
        assertFalse(whale.isAwareOfDeath());
    }
}
