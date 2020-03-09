package com.cloud.user.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Slf4j
//@Aspect
//@Component
public class LogBackAop {
    /**
     * SpringAop @Around执行两次解决方案
     *
     */
    @Pointcut("execution(public * com.cloud.user.demo.redis.Sender.*(..))")
    public void pointCut() {
    }


    @Before("pointCut()")
    public void before(JoinPoint joinPoint) throws Throwable {
        String uid = UUID.randomUUID().toString();
        MDC.put("trace_uuid", uid );
        log.info("logback设置唯一跟踪trace_uid:{}",uid);
    }
}
