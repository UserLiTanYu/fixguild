package com.fixguild.common.aspect;

import com.fixguild.common.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * {@code @IgnoreTenant} 切面：方法执行期间关闭租户过滤，结束后恢复原状态。
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Slf4j
@Aspect
@Component
public class IgnoreTenantAspect {

    /**
     * 在方法执行期间忽略租户过滤，执行结束后恢复。
     *
     * @param joinPoint 切点
     * @return 方法返回值
     */
    @Around("@annotation(com.fixguild.common.annotation.IgnoreTenant) "
            + "|| @within(com.fixguild.common.annotation.IgnoreTenant)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        boolean previous = TenantContext.isIgnore();
        TenantContext.ignore();
        log.debug("忽略租户过滤: {}", joinPoint.getSignature().toShortString());
        try {
            return joinPoint.proceed();
        } finally {
            TenantContext.setIgnore(previous);
        }
    }
}
