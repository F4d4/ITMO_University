package org.example.aop;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * Интерцептор для пессимистической блокировки операций импорта
 * Использует ReentrantLock для синхронизации доступа
 */
@Interceptor
@ImportLock
@Priority(Interceptor.Priority.APPLICATION)
public class ImportLockInterceptor {

    private static final Logger LOGGER = Logger.getLogger(ImportLockInterceptor.class.getName());
    
    // Глобальные блокировки для разных ресурсов
    private static final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        ImportLock annotation = context.getMethod().getAnnotation(ImportLock.class);
        String lockName = annotation != null ? annotation.value() : "default";
        
        ReentrantLock lock = locks.computeIfAbsent(lockName, k -> new ReentrantLock(true));
        
        LOGGER.info("[AOP] Попытка получить блокировку '" + lockName + "' для метода " + 
                    context.getMethod().getName());
        
        lock.lock();
        try {
            LOGGER.info("[AOP] Блокировка '" + lockName + "' получена для метода " + 
                        context.getMethod().getName());
            
            long startTime = System.currentTimeMillis();
            Object result = context.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            LOGGER.info("[AOP] Метод " + context.getMethod().getName() + 
                        " выполнен за " + duration + " мс");
            
            return result;
        } finally {
            lock.unlock();
            LOGGER.info("[AOP] Блокировка '" + lockName + "' освобождена для метода " + 
                        context.getMethod().getName());
        }
    }
}

