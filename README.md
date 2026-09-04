# annotation-demo

一个为了理解 **Java 注解、反射、IOC、DI、AOP 和 Spring 核心原理** 而手写的 Mini Spring 学习项目。

这个项目不依赖 Spring，也不使用第三方框架，而是基于 Java 原生能力一步步实现：

- 自定义注解
- 反射读取注解
- 反射调用方法
- JDK 动态代理
- AOP 方法增强
- IOC 容器
- `@MyAutowired` 依赖注入
- `@MyComponent` Bean 注册
- ClassPath 包扫描

> 目标不是重新实现完整的 Spring，而是通过一个足够小、能够 Debug 的项目，把 Spring 中“注解为什么会生效”这件事真正串起来。

---

## 1. 项目核心思想

这个项目最重要的一句话：

> **注解本身不会执行任何业务逻辑，真正让注解生效的是读取、解析并处理注解的代码。**

例如：

```java
@MyLog("查询用户")
public void getUser() {
    System.out.println("正在查询用户...");
}
```

`@MyLog` 自己不会打印日志。

真正的执行过程是：

```text
调用代理对象的方法
        ↓
JDK Dynamic Proxy
        ↓
MyInvocationHandler.invoke()
        ↓
找到真实实现类的方法
        ↓
反射读取 @MyLog
        ↓
执行日志增强
        ↓
反射调用真实业务方法
```

同理：

```java
@MyAutowired
private UserService userService;
```

`@MyAutowired` 本身也不会自动创建或注入对象。

真正完成注入的是：

```java
field.setAccessible(true);
field.set(target, dependency);
```

因此整个项目可以理解成：

```text
注解
  +
反射
  +
ClassLoader / 包扫描
  +
IOC 容器
  +
动态代理
  =
一个极简版 Spring 核心流程
```

---

## 2. 已实现功能

目前项目实现了以下能力：

| 功能 | 实现方式 | 对应 Spring 概念 |
| --- | --- | --- |
| 自定义组件注解 | `@MyComponent` | `@Component` |
| 自定义依赖注入注解 | `@MyAutowired` | `@Autowired` |
| 自定义日志注解 | `@MyLog` | AOP 注解 |
| Bean 创建 | 反射调用无参构造器 | Bean 实例化 |
| IOC 容器 | `Map<Class<?>, Object>` | ApplicationContext |
| 依赖注入 | Field + Reflection | DI |
| 方法增强 | JDK Dynamic Proxy | Spring AOP |
| 调用拦截 | `InvocationHandler` | MethodInterceptor 思想 |
| 包扫描 | ClassLoader + `.class` 扫描 | Component Scan |

---

## 3. 技术栈

项目只使用 Java 原生能力：

```text
Java
Reflection
Annotation
ClassLoader
JDK Dynamic Proxy
Collection
```

没有使用：

```text
Spring
Spring Boot
Maven 第三方依赖
Gradle 第三方依赖
```

建议使用：

```text
JDK 17+
IntelliJ IDEA
```

JDK 21 也可以。

---

## 4. 项目结构

```text
annotation-demo
└── src
    └── com
        └── demo
            ├── Main.java
            │
            ├── annotation
            │   ├── MyAutowired.java
            │   ├── MyComponent.java
            │   └── MyLog.java
            │
            ├── aop
            │   ├── MyInvocationHandler.java
            │   └── ProxyFactory.java
            │
            ├── context
            │   └── MyApplicationContext.java
            │
            ├── processor
            │   └── AnnotationProcessor.java
            │
            ├── scanner
            │   └── ClassScanner.java
            │
            └── service
                ├── OrderService.java
                ├── UserService.java
                └── UserServiceImpl.java
```

各模块职责：

```text
annotation
    ↓
定义框架能够识别的元数据

scanner
    ↓
扫描指定包中的 .class 文件

context
    ↓
创建 Bean、管理 Bean、执行依赖注入

aop
    ↓
给需要增强的 Bean 创建 JDK 动态代理

service
    ↓
用于验证 IOC / DI / AOP 的业务代码
```

---

## 5. 自定义注解

### 5.1 `@MyComponent`

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyComponent {
}
```

使用：

```java
@MyComponent
public class OrderService {
}
```

作用：

> 告诉 `MyApplicationContext`：这个类需要由 IOC 容器负责创建和管理。

对应 Spring：

```java
@Component
```

---

### 5.2 `@MyAutowired`

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyAutowired {
}
```

使用：

```java
@MyAutowired
private UserService userService;
```

作用：

> IOC 容器扫描字段后，根据字段类型从 Bean 容器中寻找依赖，并通过反射完成赋值。

对应 Spring：

```java
@Autowired
```

---

### 5.3 `@MyLog`

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyLog {

    String value() default "";
}
```

使用：

```java
@MyLog("查询用户")
@Override
public void getUser() {
    System.out.println("正在查询用户...");
}
```

作用：

> 标记需要进行日志增强的方法。

它不会自己执行日志逻辑，真正处理它的是：

```text
MyInvocationHandler
```

---

## 6. IOC 容器

项目中的 IOC 容器：

```java
private final Map<Class<?>, Object> beans = new HashMap<>();
```

可以简单理解为：

```text
Class 类型
    ↓
对应 Bean 对象
```

例如：

```text
UserService.class
        ↓
UserService 的代理对象

OrderService.class
        ↓
OrderService 对象
```

外部通过：

```java
context.getBean(OrderService.class);
```

获取 Bean。

本质上类似：

```java
beans.get(OrderService.class);
```

---

## 7. Bean 创建过程

`MyApplicationContext` 启动后会先扫描所有 Class：

```java
List<Class<?>> classes =
        ClassScanner.scan(basePackage);
```

随后检查：

```java
clazz.isAnnotationPresent(MyComponent.class)
```

只有带：

```java
@MyComponent
```

的类才会进入 IOC 容器。

Bean 使用反射创建：

```java
Object target =
        clazz.getDeclaredConstructor().newInstance();
```

也就是说，业务代码不再负责：

```java
new UserServiceImpl();
new OrderService();
```

对象的创建权转移给了容器。

---

## 8. 为什么这叫 IOC？

IOC：

```text
Inversion of Control
控制反转
```

以前：

```text
业务代码
   ↓
new 对象
   ↓
管理对象关系
```

现在：

```text
MyApplicationContext
   ↓
创建对象
   ↓
保存对象
   ↓
注入依赖
   ↓
业务代码从容器获取对象
```

也就是：

```text
对象创建的控制权
业务代码
   ↓
反转给
IOC 容器
```

---

## 9. `@MyAutowired` 是怎么生效的？

例如：

```java
@MyComponent
public class OrderService {

    @MyAutowired
    private UserService userService;
}
```

IOC 容器首先拿到字段：

```java
Field[] fields = clazz.getDeclaredFields();
```

判断：

```java
field.isAnnotationPresent(MyAutowired.class)
```

然后根据字段类型：

```java
field.getType()
```

得到：

```java
UserService.class
```

再从 IOC 容器查找：

```java
Object dependency =
        beans.get(field.getType());
```

最后：

```java
field.setAccessible(true);
field.set(target, dependency);
```

真正完成：

```text
OrderService.userService
        =
UserService Bean
```

因此可以把当前版本的 `@MyAutowired` 粗略理解成：

```text
扫描字段
   ↓
发现 @MyAutowired
   ↓
获得字段类型
   ↓
从 Map 找 Bean
   ↓
反射给 private 字段赋值
```

---

## 10. 为什么要先创建全部 Bean，再注入？

当前容器启动顺序：

```java
createBeans(classes);
injectDependencies();
```

而不是创建一个 Bean 后马上注入。

原因是：

假设先创建：

```text
OrderService
```

但它依赖：

```text
UserService
```

如果 `UserServiceImpl` 还没有被创建，那么：

```java
beans.get(UserService.class)
```

得到的就是：

```text
null
```

所以当前项目采用两阶段：

```text
第一阶段
扫描并实例化全部 Bean
        ↓
第二阶段
统一进行依赖注入
```

---

## 11. AOP 与 JDK 动态代理

`UserServiceImpl`：

```java
@MyComponent
public class UserServiceImpl implements UserService {

    @MyLog("查询用户")
    @Override
    public void getUser() {
        System.out.println("正在查询用户...");
    }
}
```

IOC 创建原始对象：

```text
UserServiceImpl target
```

之后检查方法是否存在：

```java
@MyLog
```

如果存在，并且目标类实现了接口：

```java
if (needProxy && clazz.getInterfaces().length > 0)
```

则调用：

```java
ProxyFactory.createProxy(target);
```

最终：

```text
原始对象 target
      ↓
JDK Dynamic Proxy
      ↓
代理对象 exposedBean
      ↓
放入 IOC 容器
```

---

## 12. JDK 动态代理调用链

代理通过：

```java
Proxy.newProxyInstance(...)
```

创建。

所有代理方法调用都会进入：

```java
MyInvocationHandler.invoke(...)
```

调用：

```java
userService.getUser();
```

实际流程：

```text
userService.getUser()
        ↓
JDK Proxy
        ↓
MyInvocationHandler.invoke()
        ↓
target.getClass().getMethod(...)
        ↓
找到 UserServiceImpl.getUser()
        ↓
targetMethod.getAnnotation(MyLog.class)
        ↓
发现 @MyLog("查询用户")
        ↓
打印 [日志开始]
        ↓
targetMethod.invoke(target, args)
        ↓
执行真实业务方法
        ↓
打印 [日志结束]
```

因此输出：

```text
[日志开始] 查询用户
正在查询用户...
[日志结束]
```

---

## 13. 为什么不直接读取 `method` 上的 `@MyLog`？

`InvocationHandler` 中收到的：

```java
Method method
```

通常来自：

```text
UserService 接口
```

但项目中的注解写在：

```text
UserServiceImpl 实现类
```

上。

所以：

```java
method.getAnnotation(MyLog.class)
```

可能读不到注解。

项目重新找到真实实现类方法：

```java
Method targetMethod =
        target.getClass().getMethod(
                method.getName(),
                method.getParameterTypes()
        );
```

然后：

```java
MyLog myLog =
        targetMethod.getAnnotation(MyLog.class);
```

这也是理解 Spring AOP 时很重要的一个问题：

> 当前拿到的方法究竟是接口 Method，还是目标类 Method？

---

## 14. 包扫描原理

启动 IOC：

```java
new MyApplicationContext("com.demo");
```

扫描器首先将：

```text
com.demo
```

转换为：

```text
com/demo
```

代码：

```java
String packagePath =
        packageName.replace(".", "/");
```

然后通过当前线程的 ClassLoader：

```java
ClassLoader classLoader =
        Thread.currentThread().getContextClassLoader();
```

找到 classpath 中对应目录：

```java
URL resource =
        classLoader.getResource(packagePath);
```

递归查找所有：

```text
.class
```

文件。

例如：

```text
com/demo/service/UserServiceImpl.class
```

会转换成全限定类名：

```text
com.demo.service.UserServiceImpl
```

然后：

```java
Class.forName(className);
```

加载成：

```java
Class<?>
```

对象。

后面的：

```text
检查注解
反射实例化
反射字段
反射方法
```

全部建立在这个 `Class<?>` 对象之上。

---

## 15. 当前完整启动流程

现在整个 Mini Spring 的启动过程可以画成：

```text
new MyApplicationContext("com.demo")
                │
                ↓
        ClassScanner.scan()
                │
                ↓
        扫描所有 .class
                │
                ↓
        Class.forName(...)
                │
                ↓
        得到 List<Class<?>>
                │
                ↓
        检查 @MyComponent
                │
                ↓
        反射创建 target
                │
                ↓
        检查方法上的 @MyLog
                │
                ↓
       ┌────────┴────────┐
       │                 │
   不需要 AOP        需要 AOP
       │                 │
   使用 target        创建 JDK Proxy
       │                 │
       └────────┬────────┘
                ↓
          放入 beans Map
                │
                ↓
        扫描 @MyAutowired
                │
                ↓
        根据字段类型找 Bean
                │
                ↓
          field.set(...)
                │
                ↓
           依赖注入完成
                │
                ↓
           getBean(...)
                │
                ↓
            执行业务
```

---

## 16. 示例业务代码

### `UserService`

```java
public interface UserService {

    void getUser();

    void deleteUser();
}
```

### `UserServiceImpl`

```java
@MyComponent
public class UserServiceImpl implements UserService {

    @MyLog("查询用户")
    @Override
    public void getUser() {
        System.out.println("正在查询用户...");
    }

    @Override
    public void deleteUser() {
        System.out.println("正在删除用户...");
    }
}
```

### `OrderService`

```java
@MyComponent
public class OrderService {

    @MyAutowired
    private UserService userService;

    public void createOrder() {
        System.out.println("正在创建订单...");
        userService.getUser();
    }
}
```

这里没有：

```java
new UserServiceImpl();
```

`UserService` 由 IOC 容器自动注入。

---

## 17. 如何运行

### 1. 用 IDEA 打开项目

直接使用 IntelliJ IDEA 打开：

```text
annotation-demo
```

建议配置：

```text
JDK 17+
```

---

### 2. 检查 `Main.java` 的扫描包

由于组件位于：

```text
com.demo.service
```

因此应该扫描它们共同的父包：

```java
MyApplicationContext context =
        new MyApplicationContext("com.demo");
```

完整示例：

```java
package com.demo;

import com.demo.context.MyApplicationContext;
import com.demo.service.OrderService;

public class Main {

    public static void main(String[] args) {

        MyApplicationContext context =
                new MyApplicationContext("com.demo");

        OrderService orderService =
                context.getBean(OrderService.class);

        orderService.createOrder();
    }
}
```


---

### 3. 运行 `Main`

IDEA：

```text
Main.java
→ 右键
→ Run 'Main.main()'
```

预期输出：

```text
正在创建订单...
[日志开始] 查询用户
正在查询用户...
[日志结束]
```

---

## 18. 推荐的 Debug 断点

这个项目最适合通过 Debug 学习，而不是只看最终输出。

推荐按照以下顺序打断点：

### 断点 1：包扫描

```java
Class<?> clazz =
        Class.forName(className);
```

观察：

```text
className
clazz
```

理解 `.class` 文件如何进入 JVM 反射世界。

### 断点 2：Bean 实例化

```java
Object target =
        clazz.getDeclaredConstructor().newInstance();
```

观察：

```text
clazz
target
```

理解 IOC 如何代替业务代码执行 `new`。

### 断点 3：代理创建

```java
return ProxyFactory.createProxy(target);
```

观察：

```text
target
exposedBean
```

比较原始 Bean 与代理 Bean。

### 断点 4：依赖注入

```java
field.set(target, dependency);
```

观察：

```text
field
target
dependency
```

这里可以直接看到：

```text
OrderService.userService
```

是如何被反射赋值的。

### 断点 5：AOP 拦截

```java
public Object invoke(
        Object proxy,
        Method method,
        Object[] args
)
```

观察调用顺序：

```text
Main
 ↓
OrderService.createOrder()
 ↓
Proxy
 ↓
MyInvocationHandler.invoke()
 ↓
UserServiceImpl.getUser()
```

---

## 19. `AnnotationProcessor` 是什么？

项目中还保留了：

```text
processor/AnnotationProcessor.java
```

这是学习过程中的早期版本。

它采用：

```java
AnnotationProcessor.process(target);
```

主动扫描对象的方法：

```text
获取 Class
   ↓
获取 Method[]
   ↓
检查 @MyLog
   ↓
读取注解
   ↓
反射执行方法
```

这种方式可以理解注解最基础的工作机制，但存在一个问题：

> 必须手动调用 Processor。

后来引入：

```text
JDK Dynamic Proxy
```

以后，就不需要业务代码主动调用处理器了。

因此可以把项目的演进理解为：

```text
AnnotationProcessor
        ↓
手动处理注解
        ↓
Dynamic Proxy
        ↓
自动拦截调用
        ↓
AOP
```

---

## 20. 当前项目的局限

这个项目是为了学习原理而刻意保持简单，目前还不是一个完整框架。

### 20.1 包扫描只适合普通文件目录

当前：

```java
new File(resource.getFile())
```

主要处理：

```text
file:
```

形式的 classpath。

项目打成 JAR 后：

```text
xxx.jar!/com/demo/...
```

目前的扫描器无法完整处理。

真正的框架还需要支持：

```text
file:
jar:
classpath:
```

等资源。

---

### 20.2 AOP 只能代理实现接口的类

当前使用：

```text
JDK Dynamic Proxy
```

因此：

```java
clazz.getInterfaces().length > 0
```

才可以代理。

如果一个类没有实现接口：

```java
@MyComponent
public class ProductService {

    @MyLog
    public void test() {
    }
}
```

当前版本不会生成代理。

Spring 中还可以使用类似 CGLIB 的子类代理方式。

---

### 20.3 暂不支持多个同类型 Bean

假设：

```java
@MyComponent
public class UserServiceImpl
        implements UserService {
}
```

又增加：

```java
@MyComponent
public class VipUserServiceImpl
        implements UserService {
}
```

当前代码：

```java
beans.put(UserService.class, exposedBean);
```

后注册的 Bean 会覆盖之前的 Bean。

当前没有实现：

```text
@Qualifier
@Primary
Bean Name
重复 Bean 检测
```

---

### 20.4 暂不支持循环依赖

例如：

```text
A
↓
@Autowired B

B
↓
@Autowired A
```

当前项目没有实现 Spring 中更加完整的循环依赖处理机制。

---

### 20.5 只支持字段注入

当前：

```java
@Target(ElementType.FIELD)
public @interface MyAutowired {
}
```

因此只实现了：

```java
@MyAutowired
private UserService userService;
```

暂未实现：

```text
构造器注入
Setter 注入
```

---

### 20.6 JDK 代理 Bean 应优先通过接口获取

`UserServiceImpl` 被 JDK 动态代理后，容器中暴露的是代理对象。

代理对象实现：

```java
UserService
```

但它并不是：

```java
UserServiceImpl
```

的实例。

因此应优先：

```java
UserService userService =
        context.getBean(UserService.class);
```

而不是：

```java
UserServiceImpl userService =
        context.getBean(UserServiceImpl.class);
```

否则代理场景下可能出现类型转换问题。

---

## 21. 项目演进路线

这个项目可以继续按照下面的路线扩展：

```text
自定义注解
    ✅
    ↓
反射解析注解
    ✅
    ↓
反射调用方法
    ✅
    ↓
JDK 动态代理
    ✅
    ↓
@MyLog AOP
    ✅
    ↓
@MyComponent
    ✅
    ↓
IOC 容器
    ✅
    ↓
@MyAutowired
    ✅
    ↓
DI 依赖注入
    ✅
    ↓
ClassPath 包扫描
    ✅
    ↓
Bean Name
    ↓
@MyQualifier
    ↓
重复 Bean 检测
    ↓
BeanDefinition
    ↓
BeanPostProcessor
    ↓
Bean 生命周期
    ↓
构造器注入
    ↓
循环依赖
    ↓
更完整的 AOP
```

---

## 22. 和 Spring 的概念对应

可以先建立下面这张对应关系：

```text
当前项目                      Spring
------------------------------------------------
@MyComponent                 @Component
@MyAutowired                 @Autowired
@MyLog                       自定义 AOP 注解
MyApplicationContext         ApplicationContext
beans Map                    Singleton Bean 容器的简化理解
ClassScanner                 Component Scan
ProxyFactory                 ProxyFactory 思想
MyInvocationHandler          MethodInterceptor 思想
createBeans                  Bean 实例化阶段
injectDependencies           属性填充 / DI
createProxyIfNecessary       Bean 后置增强思想
```

需要注意：

> 这里只是为了帮助理解概念建立的“简化对应”，Spring 的真实实现远比当前项目复杂。

---

## 23. 最终理解

完成这个项目后，可以把 Spring 注解背后的核心思想粗略理解成：

```text
@Component
    ↓
框架扫描 Class
    ↓
识别注解元数据
    ↓
创建 Bean
    ↓
放入 IOC 容器


@Autowired
    ↓
框架扫描字段 / 构造器
    ↓
寻找合适 Bean
    ↓
完成依赖注入


@Transactional / AOP Annotation
    ↓
框架发现需要增强
    ↓
创建代理对象
    ↓
方法调用被拦截
    ↓
执行前置逻辑
    ↓
执行目标方法
    ↓
执行后置逻辑
```

注解只是入口。

真正让一切运行起来的是：

```text
扫描
+
反射
+
容器
+
代理
+
生命周期管理
```

---

## 24. 项目定位

本项目适合：

- 正在学习 Java 注解和反射
- 想理解 Spring IOC / DI
- 想理解 Spring AOP 和动态代理
- 会使用 Spring，但想知道注解底层为什么能够生效
- 准备阅读 Spring 源码前，希望先建立整体模型

不建议将当前代码直接用于生产环境。

它最大的价值是：

> **代码足够少，因此可以把每一步都放进 IDEA Debug 里亲眼看见。**

---

## License

本项目主要用于 Java / Spring 原理学习，可自由修改和扩展。
