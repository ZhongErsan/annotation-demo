package com.demo.annotation;
@MyComponent
public class OrderService {
    @MyAutowired
    private UserService userService;
    public void createOrder(){
        System.out.println("正在创建订单...");
        userService.getUser();
    }
}
