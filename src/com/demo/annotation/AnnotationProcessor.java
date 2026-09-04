package com.demo.annotation;

import java.lang.reflect.Method;
//需要手动扫描，手动触发process，一次性执行完所有带有注解的方法
//主函数实现：
//UserServiceImpl target = new UserServiceImpl();
//System.out.println("====调用process====");
//AnnotationProcessor.process(target); //这里扫描+执行带注解方法
public class AnnotationProcessor {
    public static void process(Object object){
        //获取运行时对象实例
        Class<?> clazz=object.getClass();
        //反射机制获得类里面的所有方法
        Method[] methods=clazz.getDeclaredMethods();
        for(Method method:methods){
            //判断该方法上是否有MyLog的注解
            if(method.isAnnotationPresent(MyLog.class)){
                MyLog myLog=method.getAnnotation(MyLog.class);
                System.out.println("发现 @MyLog注解");
                System.out.println("注解内容："+myLog.value());
                System.out.println("方法名称："+method.getName());
                try{
                    method.invoke(object);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}
