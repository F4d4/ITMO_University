package org.example.aop;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Интерцептор для логирования вызовов методов (AOP)
 */
@Interceptor
@Logged
@Priority(Interceptor.Priority.APPLICATION + 100)
public class LoggingInterceptor {

    private static final Logger LOGGER = Logger.getLogger(LoggingInterceptor.class.getName());

    @AroundInvoke
    public Object logMethodCall(InvocationContext context) throws Exception {
        String className = context.getTarget().getClass().getSimpleName();
        String methodName = context.getMethod().getName();
        Object[] params = context.getParameters();
        
        LOGGER.info("[AOP LOG] Вызов метода: " + className + "." + methodName + 
                    "(" + Arrays.toString(params) + ")");
        
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = context.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            LOGGER.info("[AOP LOG] Метод " + className + "." + methodName + 
                        " завершен успешно за " + duration + " мс");
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            LOGGER.severe("[AOP LOG] Метод " + className + "." + methodName + 
                          " завершен с ошибкой за " + duration + " мс: " + e.getMessage());
            
            throw e;
        }
    }
}

