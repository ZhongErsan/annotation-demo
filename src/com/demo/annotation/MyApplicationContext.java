package com.demo.annotation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
//IOC的底层实现原理
public class MyApplicationContext {
    //IOC容器
    private final Map<Class<?>, Object> beans=new HashMap<>();
    //保存原始对象
    private final List<Object> targets=new ArrayList<>();
    //...表示可变参数，可以传多个类
    public MyApplicationContext(String basePackage){
        //扫描包
        List<Class<?>> classes=ClassScanner.scan(basePackage);
        //创建所有Bean
        createBeans(classes);
        //进行依赖注入
        injectDependencies();
    }

    private void createBeans(List<Class<?>> classes){
        for(Class<?> clazz:classes){
            //没有@MyComponent,就不归IOC管
            if(!clazz.isAnnotationPresent(MyComponent.class)){
                continue;
            }
            try{
                //1.反射调用无参构造器，new出原始真实对象 target
                Object target=clazz.getDeclaredConstructor().newInstance();
                //原始对象存入targets集合，用于后面注入
                targets.add(target);
                //判断是否需要AOP代理
                Object exposedBean=createProxyIfNecessary(target);
                //按照类保存
                beans.put(clazz,exposedBean);
                //按照接口保存
                for(Class<?> interfaceType:clazz.getInterfaces()){
                    beans.put(interfaceType,exposedBean);
                }
            }catch (Exception e){
                throw new RuntimeException("创建Bean失败："+clazz.getName(),e);
            }
        }
    }
    private Object createProxyIfNecessary(Object target){
        Class<?> clazz=target.getClass();
        boolean needProxy=false;
        //遍历本类所有方法，只要任意一个方法带有@MyLog，标记需要代理
        for(Method method:clazz.getDeclaredMethods()){
            if(method.isAnnotationPresent(MyLog.class)){
                needProxy=true;
                break;
            }
        }
        // 需要代理 并且 目标类实现了接口（JDK动态代理硬性条件）
        if(needProxy&&clazz.getInterfaces().length>0){
            return ProxyFactory.createProxy(target);
        }
        return target;
    }
    private void injectDependencies(){
        //遍历全部原始target对象（注意：遍历的是原始对象，不是代理对象！给原始对象的字段赋值）
        for(Object target:targets){
            Class<?> clazz=target.getClass();
            Field[] fields=clazz.getDeclaredFields();
            for(Field field:fields){
                if(!field.isAnnotationPresent(MyAutowired.class)){
                    continue;
                }
                //根据字段类型，去beans Map里面找已经创建好的Bean
                Object dependency=beans.get(field.getType());
                if(dependency==null){
                    throw new RuntimeException("找不到依赖："+field.getType().getName());
                }
                try{
                    //绕过private权限
                    field.setAccessible(true);
                    //真正完成注入
                    field.set(target,dependency);
                }catch (IllegalAccessException e){
                    throw new RuntimeException("依赖注入失败："+field.getName(),e);
                }
            }
        }
    }
    //getBean () 对外获取 Bean
    public <T> T getBean(Class<T> type){
        Object bean=beans.get(type);
        if(bean==null){
            throw new RuntimeException("IOC容器不存在："+type.getName());
        }
        return type.cast(bean);//强转返回
    }
}
