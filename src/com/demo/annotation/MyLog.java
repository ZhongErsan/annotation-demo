package com.demo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
//@interface代表这是一个自定义注解类
//@Target规定这个注解可以标注在哪里，这里表示只能标注在方法上
//@Retention控制注解存活在哪个阶段，这里是运行时注解
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyLog {
    //这是注解的属性，value是他的属性名称，不赋值时，默认为空字符串
    String value() default "";
}
