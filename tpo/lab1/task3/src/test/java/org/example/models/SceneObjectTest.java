package org.example.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SceneObjectTest {

    @Test
    void testGetName() {
        SceneObject obj = new SceneObject("Кит");
        assertEquals("Кит", obj.getName());
    }

    @Test
    void testNullNameThrows() {
        assertThrows(IllegalArgumentException.class, () -> new SceneObject(null));
    }

    @Test
    void testBlankNameThrows() {
        assertThrows(IllegalArgumentException.class, () -> new SceneObject("   "));
    }
}
