package com.saas.framework.common.annotation;

import java.lang.annotation.*;

/**
 * 权限校验注解
 * 标记在 Controller 方法上，AOP 切面会自动校验当前用户是否拥有指定权限
 * <p>
 * 使用示例：@RequirePermission("student:add")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /** 权限编码，如 "student:list", "student:add" */
    String value();
}
