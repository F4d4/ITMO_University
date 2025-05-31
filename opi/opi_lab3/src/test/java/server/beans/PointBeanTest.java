package server.beans;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class PointBeanTest {

    private PointBean pointBean;

    @Before
    public void setUp() {
        pointBean = new PointBean();
    }

    @Test
    public void testSetAndGetX() {
        Double x = 1.5;
        pointBean.setX(x);
        assertEquals(x, pointBean.getX());
    }

    @Test
    public void testSetAndGetY() {
        Double y = 2.5;
        pointBean.setY(y);
        assertEquals(y, pointBean.getY());
    }

    @Test
    public void testSetAndGetR() {
        Double r = 3.0;
        pointBean.setR(r);
        assertEquals(r, pointBean.getR());
    }
} 