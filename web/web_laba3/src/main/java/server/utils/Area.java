package server.utils;

public class Area {
    public static class Validate {
        public static boolean validateX(double x) {
            return x >= -5 && x <= 5;
        }

        public static boolean validateY(double y) {
            return y >= -5 && y <= 5;
        }

        public static boolean validateR(double r) {
            return r >0 && r <= 5;
        }
    }

    public static class Check{
        public static boolean calculate(double x, double y, double r) {
            if (x >= 0 && y >= 0 && x <= r && y <= r / 2) {
                return true;
            }
            if (x <= 0 && y >= 0 && x >= -r && y <= r / 2 && (0.5 * x - y + r * 0.5) >= 0) {
                return true;
            }
            return x <= 0 && y <= 0 && x >= -r && y >= -r && (Math.pow(x, 2) + Math.pow(y, 2) <= Math.pow(r, 2));
        }
    }


}
