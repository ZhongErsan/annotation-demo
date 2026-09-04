package com.demo.service;

import com.demo.annotation.MyComponent;
import com.demo.annotation.MyLog;

@MyComponent
public class UserServiceImpl implements UserService {
    @MyLog("查询用户")
    @Override
    public void getUser(){
        System.out.println("正在查询用户...");
    }
    @Override
    public void deleteUser(){
        System.out.println("正在删除用户...");
    }
}
