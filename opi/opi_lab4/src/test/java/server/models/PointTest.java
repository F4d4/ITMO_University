package server.models;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class PointTest {

    private Point point;

    @Before
    public void setUp() {
        point = new Point(true, 1.0, 2.0, 3.0, "test-date", 100L);
    }

    @Test
    public void testGetX() {
        assertEquals(1.0, point.getX(), 0.01);
    }

    @Test
    public void testGetY() {
        assertEquals(2.0, point.getY(), 0.01);
    }

    @Test
    public void testGetR() {
        assertEquals(3.0, point.getR(), 0.01);
    }

    @Test
    public void testGetRes() {
        assertTrue(point.getRes());
    }

    @Test
    public void testGetDateOfRequest() {
        assertEquals("test-date", point.getDateOfRequest());
    }

    @Test
    public void testGetExecutionTime() {
        assertEquals(100L, point.getExecutionTime());
    }

    @Test
    public void testSetX() {
        point.setX(4.0);
        assertEquals(4.0, point.getX(), 0.01);
    }

    @Test
    public void testSetY() {
        point.setY(5.0);
        assertEquals(5.0, point.getY(), 0.01);
    }

    @Test
    public void testSetR() {
        point.setR(6.0);
        assertEquals(6.0, point.getR(), 0.01);
    }

    @Test
    public void testSetRes() {
        point.setRes(false);
        assertFalse(point.getRes());
    }

    @Test
    public void testSetDateOfRequest() {
        point.setDateOfRequest("new-date");
        assertEquals("new-date", point.getDateOfRequest());
    }

    @Test
    public void testSetExecutionTime() {
        point.setExecutionTime(200L);
        assertEquals(200L, point.getExecutionTime());
    }

    @Test
    public void testPointCreationWithRandomValues() {
        // Fuzzing test with random values
        for (int i = 0; i < 100; i++) {
            double x = Math.random() * 1000 * (Math.random() > 0.5 ? 1 : -1);
            double y = Math.random() * 1000 * (Math.random() > 0.5 ? 1 : -1);
            double r = Math.random() * 1000;
            boolean res = Math.random() > 0.5;
            String dateOfRequest = "TestDate" + i;
            long executionTime = (long) (Math.random() * 100000);

            Point point = new Point(res, x, y, r, dateOfRequest, executionTime);

            assertEquals(x, point.getX(), 0.0001);
            assertEquals(y, point.getY(), 0.0001);
            assertEquals(r, point.getR(), 0.0001);
            assertEquals(res, point.getRes());
            assertEquals(dateOfRequest, point.getDateOfRequest());
            assertEquals(executionTime, point.getExecutionTime());
        }
    }

    @Test
    public void testPointWithBoundaryValues() {
        // Fuzzing test with boundary values
        double[] boundaryValues = {Double.MIN_VALUE, Double.MAX_VALUE, 0.0, -0.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (double x : boundaryValues) {
            for (double y : boundaryValues) {
                for (double r : boundaryValues) {
                    Point point = new Point(true, x, y, r, "BoundaryTest", 100L);
                    assertEquals(x, point.getX(), 0.0);
                    assertEquals(y, point.getY(), 0.0);
                    assertEquals(r, point.getR(), 0.0);
                }
            }
        }
    }
} 