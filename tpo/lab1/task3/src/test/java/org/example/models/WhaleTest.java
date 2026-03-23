package org.example.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WhaleTest {

    @Test
    void testWhaleCreation() {
        Whale whale = new Whale("Кит");
        assertEquals("Кит", whale.getName());
    }

    @Test
    void testInitialPositionIsUnnatural() {
        Whale whale = new Whale("Кит");
        assertAll(
                () -> assertEquals(Position.UNNATURAL, whale.getPosition()),
                () -> assertFalse(whale.isInNaturalPosition())
        );
    }

    @Test
    void testSetPositionToNatural() {
        Whale whale = new Whale("Кит");
        whale.setPosition(Position.NATURAL);
        assertAll(
                () -> assertEquals(Position.NATURAL, whale.getPosition()),
                () -> assertTrue(whale.isInNaturalPosition())
        );
    }

    @Test
    void testSetPositionNullThrows() {
        Whale whale = new Whale("Кит");
        assertThrows(NullPointerException.class, () -> whale.setPosition(null));
    }

    @Test
    void testInitiallyNotAwareOfExistence() {
        Whale whale = new Whale("Кит");
        assertFalse(whale.isAwareOfExistence());
    }

    @Test
    void testBecomeAwareOfExistence() {
        Whale whale = new Whale("Кит");
        whale.becomeAwareOfExistence();
        assertTrue(whale.isAwareOfExistence());
    }

    @Test
    void testInitiallyNotAwareOfDeath() {
        Whale whale = new Whale("Кит");
        assertFalse(whale.isAwareOfDeath());
    }

    @Test
    void testBecomeAwareOfDeath() {
        Whale whale = new Whale("Кит");
        whale.becomeAwareOfDeath();
        assertTrue(whale.isAwareOfDeath());
    }

    @Test
    void testAwarenessIsIndependent() {
        Whale whale = new Whale("Кит");
        whale.becomeAwareOfExistence();
        assertAll(
                () -> assertTrue(whale.isAwareOfExistence()),
                () -> assertFalse(whale.isAwareOfDeath())
        );
    }
}
