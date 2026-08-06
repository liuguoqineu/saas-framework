package com.saas.framework.common.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    String operation() default "OTHER";

    String module() default "";

    String description() default "";
}
