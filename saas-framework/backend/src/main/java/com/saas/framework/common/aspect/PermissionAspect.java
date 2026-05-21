package com.saas.framework.common.aspect;

import com.saas.framework.common.annotation.RequirePermission;
import com.saas.framework.common.context.UserContext;
import com.saas.framework.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 权限校验 AOP 切面
 * 拦截 @RequirePermission 注解的方法，校验当前用户是否拥有指定权限
 * <p>
 * 超级账户（tenant_id=0）拥有所有权限，直接放行
 */
@Slf4j
@Aspect
@Component
public class PermissionAspect {

    @Around("@annotation(com.saas.framework.common.annotation.RequirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        // 超级账户拥有所有权限，直接放行
        if (UserContext.isSuperAdmin()) {
            return joinPoint.proceed();
        }

        // 获取注解上的权限编码
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequirePermission annotation = signature.getMethod().getAnnotation(RequirePermission.class);
        String requiredPermission = annotation.value();

        // 获取当前用户的权限列表
        List<String> userPermissions = UserContext.getPermissions();
        if (userPermissions == null || !userPermissions.contains(requiredPermission)) {
            log.warn("用户 {} 权限不足，需要权限: {}", UserContext.getUsername(), requiredPermission);
            throw new BusinessException(403, "权限不足，缺少权限: " + requiredPermission);
        }

        log.debug("用户 {} 权限校验通过: {}", UserContext.getUsername(), requiredPermission);
        return joinPoint.proceed();
    }
}
