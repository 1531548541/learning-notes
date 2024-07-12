package com.mini.spring.ioc;

/**
 * @Author: wujie
 * @Date: 2024/7/10 21:27
 */
public class UserServiceImpl {
    private String name;

    public UserServiceImpl() {
        System.out.println("UserServiceImpl");
    }

    public UserServiceImpl(String name) {
        this.name = name;
    }

    public void query() {
        System.out.println("UserServiceImpl.query:" + this.name);
    }
}
