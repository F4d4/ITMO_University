package org.example.models;

import org.junit.jupiter.api.Test;

import static org.example.models.WhaleCondition.UNUSUAL;
import static org.example.models.WhaleCondition.USUAL;
import static org.junit.jupiter.api.Assertions.*;

class WhaleTest {

    @Test
    void shouldCreateWhaleWithCorrectFields() {
        Whale whale = new Whale("кит", USUAL);

        assertEquals("кит", whale.getName());
        assertEquals(USUAL, whale.getCondition());
        assertTrue(whale.isStillWhale());
    }

    @Test
    void shouldChangeCondition() {
        Whale whale = new Whale("кит", USUAL);
        whale.setCondition(UNUSUAL);
        assertEquals(UNUSUAL, whale.getCondition());
    }

    @Test
    void shouldChangeStillWhaleFlag() {
        Whale whale = new Whale("кит", USUAL);
        whale.setStillWhale(false);
        assertFalse(whale.isStillWhale());
    }

    @Test
    void shouldThrowExceptionWhenNameIsNull() {
        assertThrows(NullPointerException.class, () -> new Whale(null, USUAL));
    }

    @Test
    void shouldThrowExceptionWhenConditionIsNull() {
        assertThrows(NullPointerException.class, () -> new Whale("кит", null));
    }

    @Test
    void shouldThrowExceptionWhenNewConditionIsNull() {
        Whale whale = new Whale("кит", USUAL);
        assertThrows(NullPointerException.class, () -> whale.setCondition(null));
    }
}