package com.itheima.mybatistest.sqlSession;

public interface SqlSessionFactory {

    /**
     * 1.生产sqlSession对象
     * 2.创建执行器对象
     */
    SqlSession openSession();
}
