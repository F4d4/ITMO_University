package server.models;

public class PointFuzzingTest {

    public static void main(String[] args) {
        System.out.println("Starting fuzzing test for Point class...");
        
        // Fuzzing test with random values
        System.out.println("Testing with random values:");
        for (int i = 0; i < 100; i++) {
            double x = Math.random() * 1000 * (Math.random() > 0.5 ? 1 : -1);
            double y = Math.random() * 1000 * (Math.random() > 0.5 ? 1 : -1);
            double r = Math.random() * 1000;
            boolean res = Math.random() > 0.5;
            String dateOfRequest = "TestDate" + i;
            long executionTime = (long) (Math.random() * 100000);

            Point point = new Point(res, x, y, r, dateOfRequest, executionTime);

            if (Math.abs(x - point.getX()) > 0.0001 || 
                Math.abs(y - point.getY()) > 0.0001 || 
                Math.abs(r - point.getR()) > 0.0001 || 
                res != point.getRes() || 
                !dateOfRequest.equals(point.getDateOfRequest()) || 
                executionTime != point.getExecutionTime()) {
                System.out.println("Test failed at iteration " + i);
                System.out.println("Expected: x=" + x + ", y=" + y + ", r=" + r + ", res=" + res + ", date=" + dateOfRequest + ", time=" + executionTime);
                System.out.println("Got: x=" + point.getX() + ", y=" + point.getY() + ", r=" + point.getR() + ", res=" + point.getRes() + ", date=" + point.getDateOfRequest() + ", time=" + point.getExecutionTime());
            }
        }
        System.out.println("Random values test completed.");

        // Fuzzing test with boundary values
        System.out.println("Testing with boundary values:");
        double[] boundaryValues = {Double.MIN_VALUE, Double.MAX_VALUE, 0.0, -0.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        int testCount = 0;
        for (double x : boundaryValues) {
            for (double y : boundaryValues) {
                for (double r : boundaryValues) {
                    testCount++;
                    Point point = new Point(true, x, y, r, "BoundaryTest", 100L);
                    if (Double.compare(x, point.getX()) != 0 || 
                        Double.compare(y, point.getY()) != 0 || 
                        Double.compare(r, point.getR()) != 0) {
                        System.out.println("Boundary test failed at test " + testCount);
                        System.out.println("Expected: x=" + x + ", y=" + y + ", r=" + r);
                        System.out.println("Got: x=" + point.getX() + ", y=" + point.getY() + ", r=" + point.getR());
                    }
                }
            }
        }
        System.out.println("Boundary values test completed. Total tests run: " + testCount);
        System.out.println("Fuzzing test for Point class finished.");
    }
} 