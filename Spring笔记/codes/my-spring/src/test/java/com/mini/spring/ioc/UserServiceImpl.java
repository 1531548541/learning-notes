package com.mini.spring.ioc;

/**
 * @Author: wujie
 * @Date: 2024/7/10 21:27
 */
public class UserServiceImpl {
    private String name;
    private String company;
    private String location;

    private UserDao userDao;

    public UserServiceImpl() {
        System.out.println("UserServiceImpl");
    }

    public UserServiceImpl(String name) {
        this.name = name;
    }

    public void query() {
        System.out.println("UserServiceImpl.query:" + this.name);
    }

    public void queryByName(String name) {
        System.out.println("UserServiceImpl.queryByName:" + userDao.selectByName(name)+";company:"+this.company+";location:"+this.location);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
