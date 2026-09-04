package com.demo.aop;

import java.lang.reflect.Proxy;

public class ProxyFactory {
    public static Object createProxy(Object target){
        return Proxy.newProxyInstance(
                //获取真实对象的class对象，然后加载目标类的类加载器
                target.getClass().getClassLoader(),
                //获取目标类实现的所有接口参数
                target.getClass().getInterfaces(),
                //调用处理器对象
                new MyInvocationHandler(target)
        );
    }
}
