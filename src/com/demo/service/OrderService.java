package com.demo.service;

import com.demo.annotation.MyAutowired;
import com.demo.annotation.MyComponent;

@MyComponent
public class OrderService {
    @MyAutowired
    private UserService userService;
    public void createOrder(){
        System.out.println("正在创建订单...");
        userService.getUser();
    }
}
