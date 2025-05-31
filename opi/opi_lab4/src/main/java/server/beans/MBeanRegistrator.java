package server.beans;

import javax.management.*;
import java.lang.management.ManagementFactory;

public class MBeanRegistrator {
    
    private static final PointStatisticsMBean pointStatisticsMBean = new PointStatisticsMBean();
    private static final PointRatioMBean pointRatioMBean = new PointRatioMBean(pointStatisticsMBean);
    
    private static boolean initialized = false;
    
    public static void registerMBeans() {
        if (initialized) {
            return;
        }
        
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            
            // Регистрация MBean'а статистики точек
            ObjectName pointStatisticsName = new ObjectName("server.beans:type=PointStatistics");
            mbs.registerMBean(pointStatisticsMBean, pointStatisticsName);
            
            // Регистрация MBean'а соотношения промахов
            ObjectName pointRatioName = new ObjectName("server.beans:type=PointRatio");
            mbs.registerMBean(pointRatioMBean, pointRatioName);
            
            initialized = true;
            
            System.out.println("MBean-компоненты успешно зарегистрированы.");
        } catch (MalformedObjectNameException | InstanceAlreadyExistsException | 
                 MBeanRegistrationException | NotCompliantMBeanException e) {
            e.printStackTrace();
        }
    }
    
    public static PointStatisticsMBean getPointStatisticsMBean() {
        return pointStatisticsMBean;
    }
    
    public static PointRatioMBean getPointRatioMBean() {
        return pointRatioMBean;
    }
} 