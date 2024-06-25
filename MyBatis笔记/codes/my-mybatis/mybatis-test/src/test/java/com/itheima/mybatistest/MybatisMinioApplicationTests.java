package com.itheima.mybatistest;

import com.itheima.mybatistest.dao.IUserDao;
import com.itheima.mybatistest.io.Resources;
import com.itheima.mybatistest.pojo.User;
import com.itheima.mybatistest.sqlSession.SqlSession;
import com.itheima.mybatistest.sqlSession.SqlSessionFactory;
import com.itheima.mybatistest.sqlSession.SqlSessionFactoryBuilder;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

public class MybatisMinioApplicationTests {

    /**
     * 使用简单的人工拼接statementId
     */
    @Test
    public void testSimple() throws Exception {
        InputStream inputStream = Resources.getResourceAsSteam("sqlMapConfig.xml");
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        SqlSession sqlSession = sqlSessionFactory.openSession();
        List<User> list = sqlSession.selectList("com.itheima.mybatistest.dao.IUserDao.findAll", null);
        for (User user1 : list) {
            System.out.println(user1);
        }
        System.out.println("--------条件查询--------");
        User user = new User();
        user.setId(1);
        user.setUsername("1");
        List<User> list2 = sqlSession.selectList("com.itheima.mybatistest.dao.IUserDao.findByCondition", user);
        for (User user1 : list2) {
            System.out.println(user1);
        }
        sqlSession.close();
    }

    /**
     * 使用mapper动态代理方式
     */
    @Test
    public void testWithMapperProxy() throws Exception {
        InputStream inputStream = Resources.getResourceAsSteam("sqlMapConfig.xml");
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        SqlSession sqlSession = sqlSessionFactory.openSession();
        IUserDao userDao = sqlSession.getMapper(IUserDao.class);
        List<User> list = userDao.findAll();
        for (User user1 : list) {
            System.out.println(user1);
        }
        System.out.println("--------条件查询--------");
        User user = new User();
        user.setId(1);
        user.setUsername("1");
        User user1 = userDao.findByCondition(user);
        System.out.println(user1);
        sqlSession.close();
    }

}
