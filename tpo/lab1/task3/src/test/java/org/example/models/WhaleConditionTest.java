package org.example.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WhaleConditionTest {

    @Test
    void usualToStringShouldNotBeEmpty() {
        assertNotNull(WhaleCondition.USUAL.toString());
        assertFalse(WhaleCondition.USUAL.toString().isBlank());
    }

    @Test
    void unusualToStringShouldNotBeEmpty() {
        assertNotNull(WhaleCondition.UNUSUAL.toString());
        assertFalse(WhaleCondition.UNUSUAL.toString().isBlank());
    }
}