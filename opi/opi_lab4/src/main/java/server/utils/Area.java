package server.utils;

public class Area {
    public static boolean calculate(double x, double y, double r) {
        if (x >= 0 && y >= 0 && x <= r && y <= r ) {
            return true;
        }
        if (x <= 0 && y <= 0 && x >= -r/2 && y >= -r / 2 && (x + y + r * 0.5) >= 0) {
            return true;
        }
        return x >= 0 && y <= 0 && y>-r/2 && x<=r/2 && Math.pow(x,2)+Math.pow(y,2)<=Math.pow(r/2,2);
    }

    public static boolean validation(double x , double y , double r ){
        if(x<-5 || x> 5){
            return false;
        }
        if(y<-5 || y> 5){
            return false;
        }
        if(r<-5 || r> 5){
            return false;
        }
        return true;
    }


}
