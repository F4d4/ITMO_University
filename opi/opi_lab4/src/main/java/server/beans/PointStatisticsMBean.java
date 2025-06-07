package server.beans;

import java.util.ArrayList;
import java.util.List;
import javax.management.*;
import javax.management.NotificationBroadcasterSupport;

public class PointStatisticsMBean extends NotificationBroadcasterSupport implements PointStatisticsMXBean {

    private int totalPoints = 0;
    private int missedPoints = 0;
    private int consecutiveMisses = 0;
    private long sequenceNumber = 1;
    
    @Override
    public int getTotalPoints() {
        return totalPoints;
    }
    
    @Override
    public int getMissedPoints() {
        return missedPoints;
    }
    
    @Override
    public int getConsecutiveMisses() {
        return consecutiveMisses;
    }
    
    public void addPoint(boolean hit) {
        totalPoints++;
        
        if (!hit) {
            missedPoints++;
            consecutiveMisses++;
            
            if (consecutiveMisses == 2) {
                sendNotification();
            }
        } else {
            consecutiveMisses = 0;
        }
    }
    
    private void sendNotification() {
        Notification notification = new Notification(
                "two.consecutive.misses",
                this,
                sequenceNumber++,
                System.currentTimeMillis(),
                "Пользователь совершил 2 промаха подряд!"
        );
        sendNotification(notification);
        System.out.println("DEBUG: Уведомление отправлено! consecutiveMisses = " + consecutiveMisses);
    }
    
    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        String[] types = new String[] {"two.consecutive.misses"};
        String name = Notification.class.getName();
        String description = "Оповещение о двух промахах подряд";
        MBeanNotificationInfo info = new MBeanNotificationInfo(types, name, description);
        return new MBeanNotificationInfo[] {info};
    }
} 