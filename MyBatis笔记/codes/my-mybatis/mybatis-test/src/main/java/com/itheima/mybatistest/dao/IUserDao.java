package com.itheima.mybatistest.dao;


import com.itheima.mybatistest.pojo.User;

import java.util.List;

public interface IUserDao {

    /**
     * 查询所有
     */
    List<User> findAll() throws Exception;

    /**
     * 根据多条件查询
     */
    User findByCondition(User user) throws Exception;



}
