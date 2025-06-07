package server.beans;
 
public interface PointStatisticsMXBean {
    int getTotalPoints();
    int getMissedPoints();
    int getConsecutiveMisses();
} 