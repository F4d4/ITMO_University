package server.beans;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class TableBeanTest {

    private TableBean tableBean;

    @Before
    public void setUp() {
        tableBean = new TableBean();
    }

    @Test
    public void testGetPointsNotNull() {
        assertNotNull("Список точек не должен быть null", tableBean.getPoints());
    }
} 