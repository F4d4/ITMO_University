package server.beans;

import java.util.ArrayList;
import java.util.List;
import javax.management.*;

public class PointStatisticsMBean implements PointStatisticsMXBean, NotificationEmitter {

    private int totalPoints = 0;
    private int missedPoints = 0;
    private int consecutiveMisses = 0;
    private final List<NotificationListener> listeners = new ArrayList<>();
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
        
        for (NotificationListener listener : listeners) {
            listener.handleNotification(notification, null);
        }
    }
    
    @Override
    public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
        listeners.add(listener);
    }
    
    @Override
    public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
        if (!listeners.remove(listener)) {
            throw new ListenerNotFoundException("Слушатель не найден");
        }
    }
    
    @Override
    public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException {
        removeNotificationListener(listener);
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