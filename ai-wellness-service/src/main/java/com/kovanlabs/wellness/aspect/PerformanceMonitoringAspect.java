package com.kovanlabs.wellness.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PerformanceMonitoringAspect {

    private static final Logger log = LoggerFactory.getLogger(PerformanceMonitoringAspect.class);
    private static final long SLOW_EXECUTION_THRESHOLD_MS = 500;

    @Around("execution(* com.kovanlabs.wellness.service.*.*(..)) || execution(* com.kovanlabs.wellness.repository.*.*(..))")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        Object result = joinPoint.proceed();

        long executionTime = System.currentTimeMillis() - start;

        if (executionTime > SLOW_EXECUTION_THRESHOLD_MS) {
            log.warn("Slow execution detected! {}.{} took {} ms",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(),
                    executionTime);
        }

        return result;
    }
}
