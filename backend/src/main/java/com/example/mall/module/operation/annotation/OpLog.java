package com.example.mall.module.operation.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OpLog {

    String module();

    String action();

    String description() default "";
}
