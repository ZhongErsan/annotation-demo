package com.demo.annotation;

/**
 * 1. 包扫描
 * `ClassScanner`扫描指定包，递归读取 class 文件，加载得到全部`Class<?>`。
 *  不是获取类名字符串，是拿到Class 反射对象。
 *
 * 2. 筛选组件，实例化 Bean（createBeans）
 * 遍历扫描出来的 Class：
 *
 * - 判断类上是否存在`@MyComponent`，没有直接跳过。
 * - 使用反射调用无参构造器：`clazz.getDeclaredConstructor().newInstance()`，创建**原始 target 对象**。
 * - AOP 判断：检查这个类的方法有没有`@MyLog`；有注解并且实现接口 → 通过 JDK 动态代理生成代理对象`exposedBean`；不需要代理就继续使用原始 target。
 * - 将 Bean 存入`beans Map`容器：key 是 Class 类型（实现类、接口），value 存对象（有可能是代理对象）；同时原始 target 存入 targets 列表，留给后续依赖注入使用。
 *
 * 3. 依赖注入（injectDependencies）
 * 遍历全部原始 target 对象（不是代理对象！代理是动态生成，没有业务类的字段）：
 *
 * - 反射拿到`Field[]`所有成员变量。
 * - 判断字段是否标记`@MyAutowired`。
 * - 根据字段的类型`field.getType()`去 beans Map 查找已经实例化完成的 Bean。
 * - `field.setAccessible(true)`暴力访问私有字段；反射`field.set(target, dependency)`完成赋值注入。
 *
 * >
 * > ⚠️手写简化版顺序：全部实例化完成之后，再统一做依赖注入，不能处理循环依赖。
 */
public class Main {
    public static void main(String[] args){
        MyApplicationContext context=new MyApplicationContext("com.demo.annotation");
        OrderService orderService=context.getBean(OrderService.class);
        orderService.createOrder();

    }
}
