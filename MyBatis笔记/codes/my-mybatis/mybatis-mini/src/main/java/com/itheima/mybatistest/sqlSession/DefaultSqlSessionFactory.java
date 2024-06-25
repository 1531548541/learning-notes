package com.itheima.mybatistest.sqlSession;

import com.itheima.mybatistest.executor.SimpleExecutor;
import com.itheima.mybatistest.pojo.Configuration;

public class DefaultSqlSessionFactory implements SqlSessionFactory {

    private Configuration configuration;

    public DefaultSqlSessionFactory(Configuration configuration) {
        this.configuration = configuration;
    }

    public SqlSession openSession() {
        //生产sqlSession对象
        return new DefaultSqlSession(configuration, new SimpleExecutor());
    }
}
