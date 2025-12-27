package org.example.aop;

import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для включения логирования статистики L2 Cache
 * Используется с CDI Interceptor
 */
@InterceptorBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheStatistics {
    /**
     * Включить логирование статистики кэша
     */
    boolean enabled() default true;
}
