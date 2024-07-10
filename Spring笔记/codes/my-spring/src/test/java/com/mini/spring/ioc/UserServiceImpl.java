package com.mini.spring.ioc;

/**
 * @Author: wujie
 * @Date: 2024/7/10 21:27
 */
public class UserServiceImpl {
    public UserServiceImpl(){
        System.out.println("UserServiceImpl");
    }

    public void query(){
        System.out.println("UserServiceImpl.query");
    }
}
