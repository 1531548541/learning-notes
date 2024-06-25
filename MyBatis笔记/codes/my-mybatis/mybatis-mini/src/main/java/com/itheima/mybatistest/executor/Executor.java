package com.itheima.mybatistest.executor;

import com.itheima.mybatistest.pojo.Configuration;
import com.itheima.mybatistest.pojo.MappedStatement;

import java.util.List;

/**
 * 真正与底层jdbc交互的执行器
 */
public interface Executor {

    <E> List<E> query(Configuration configuration, MappedStatement mappedStatement, Object param) throws Exception;

    void close();
}
