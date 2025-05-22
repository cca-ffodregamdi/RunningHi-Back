package com.runninghi.runninghibackv2.common.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class LogAspect {

    @Pointcut("within(@org.springframework.stereotype.Controller *)")
    public void controller() {
    }

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void restController() {
    }

    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void service() {
    }

    @Pointcut("execution(* com.runninghi..*(..))")
    public void allMethods() {
    }

    @Before("(controller() || restController() || service()) && allMethods()")
    public void logBefore(JoinPoint joinPoint) {
        log.info("Entering: {}.{}() with arguments[s] = {}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                Arrays.toString(joinPoint.getArgs()));
    }

    @AfterReturning(pointcut = "(controller() || restController() || service()) && allMethods()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        log.info("Exiting: {}.{}() with result = {}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                result);
    }

    @AfterThrowing(pointcut = "(controller() || restController() || service()) && allMethods()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        log.error("Exception in {}.{}() with cause = {}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                e.getCause() != null ? e.getCause() : "NULL");
    }

    @Around("(controller() || restController() || service()) && allMethods()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        Object result = joinPoint.proceed();

        stopWatch.stop();
        log.info("Ex ecution time of {}.{}(): {} ms",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                stopWatch.getTotalTimeMillis());

        return result;
    }

}

