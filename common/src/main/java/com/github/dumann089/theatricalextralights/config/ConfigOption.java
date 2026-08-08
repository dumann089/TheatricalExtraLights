package com.github.dumann089.theatricalextralights.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigOption {
    String name();
    String tooltip() default "";
    double min() default 0.0;
    double max() default 100.0;
}