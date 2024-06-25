package com.itheima.mybatistest.sqlSession;

import java.util.List;

public interface SqlSession {
    /**
     * 查询多个结果
     * sqlSession.selectList(); :定位到要执行的sql语句，从而执行
     * select * from user where username like '% ? %'
     */
    <E> List<E> selectList(String statementId, Object param) throws Exception;

    /**
     * 查询单条记录
     */
    <T> T selectOne(String statementId, Object param) throws Exception;

    /**
     * 关闭资源
     */
    void close();
    /**
     * 生成代理对象
     */
    <T> T getMapper(Class<?> mapperClass);
}
