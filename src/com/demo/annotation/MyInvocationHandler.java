package com.demo.annotation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
//JDK 动态代理规定的拦截接口
public class MyInvocationHandler implements InvocationHandler {
    private final Object target;
    public MyInvocationHandler(Object target){
        this.target=target;
    }
    //method通常代表的是UserService接口里面的方法，但是注解标注在实现类方法上
    //所以重新找到实现类方法targetMethod
    //然后获得方法上面的注解
    @Override
    //只要访问代理对象的任何方法
    //invoke就会拦下来，判断是否要做增强，再放行
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //来自真实target实现类，上面有注解
        //① 这里是反射：去真实target类获取原始方法
        Method targetMethod=target.getClass().getMethod(
                method.getName(),
                method.getParameterTypes()
        );
        //② 反射读取注解
        MyLog myLog=targetMethod.getAnnotation(MyLog.class);
        if(myLog!=null){
            System.out.println("[日志开始] "+myLog.value());
            Object result=targetMethod.invoke(target,args);
            System.out.println("[日志结束]");
            return result;
        }
        return targetMethod.invoke(target,args);
    }
}
