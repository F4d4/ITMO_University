package server.utils;

import org.junit.Test;
import static org.junit.Assert.*;

public class AreaTest {

    @Test
    public void testCalculateInsideFirstQuadrant() {
        assertTrue(Area.calculate(1.0, 1.0, 2.0));
    }

    @Test
    public void testCalculateOutsideFirstQuadrant() {
        assertFalse(Area.calculate(3.0, 3.0, 2.0));
    }

    @Test
    public void testCalculateInsideThirdQuadrantTriangle() {
        assertTrue(Area.calculate(-0.5, -0.5, 2.0));
    }

    @Test
    public void testCalculateOutsideThirdQuadrantTriangle() {
        assertFalse(Area.calculate(-1.5, -1.5, 2.0));
    }

    @Test
    public void testCalculateInsideFourthQuadrantCircle() {
        assertTrue(Area.calculate(0.5, -0.5, 2.0));
    }

    @Test
    public void testCalculateOutsideFourthQuadrantCircle() {
        assertFalse(Area.calculate(1.5, -1.5, 2.0));
    }

    @Test
    public void testValidationValidValues() {
        assertTrue(Area.validation(1.0, 1.0, 1.0));
    }

    @Test
    public void testValidationInvalidX() {
        assertFalse(Area.validation(-6.0, 1.0, 1.0));
    }

    @Test
    public void testValidationInvalidY() {
        assertFalse(Area.validation(1.0, -6.0, 1.0));
    }

    @Test
    public void testValidationInvalidR() {
        assertFalse(Area.validation(1.0, 1.0, -6.0));
    }
} 