package server;

import server.beans.MBeanRegistrator;
import server.beans.PointStatisticsMBean;

import javax.management.*;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.rmi.registry.LocateRegistry;
import java.util.Scanner;

public class MBeanMonitor {

    public static void main(String[] args) {
        try {
            // Регистрация MBean-компонентов
            MBeanRegistrator.registerMBeans();
            PointStatisticsMBean statisticsMBean = MBeanRegistrator.getPointStatisticsMBean();
            
            // Настройка JMX-сервера с RMI
            LocateRegistry.createRegistry(9999);
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi");
            JMXConnectorServer cs = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
            cs.start();
            
            System.out.println("MBean-монитор запущен. Порт: 9999");
            System.out.println("Используйте JConsole для подключения к приложению.");
            System.out.println("Сервис URL: " + url.toString());
            System.out.println("Системная информация:");
            System.out.println("ОС: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
            System.out.println("JVM: " + System.getProperty("java.vm.name") + " " + System.getProperty("java.version"));
            
            // Пример использования MBean в коде
            // Добавим тестовые точки
            statisticsMBean.addPoint(true);  // Попадание
            statisticsMBean.addPoint(false); // Промах
            statisticsMBean.addPoint(false); // Промах, должно быть уведомление
            
            // Ожидание ввода пользователя для остановки
            System.out.println("\nВведите 'exit' для остановки:");
            Scanner scanner = new Scanner(System.in);
            while (!scanner.nextLine().equals("exit")) {
                System.out.println("Введите 'exit' для остановки:");
            }
            
            // Закрываем соединения
            cs.stop();
            System.out.println("MBean-монитор остановлен.");
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 