/**
 *    Copyright ${license.git.copyrightYears} the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.itheima.test;
import com.itheima.mapper.UserMapper;
import com.itheima.pojo.User;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class MybatisTest {


  /**
   * 测试方法：传统方式
   */
  @Test
  public void test1() throws IOException {

    // 1. 通过类加载器对配置文件进行加载，加载成了字节输入流，存到内存中 注意：配置文件并没有被解析
    InputStream resourceAsStream = Resources.getResourceAsStream("sqlMapConfig.xml");

    // 2. (1)解析了配置文件，封装configuration对象 （2）创建了DefaultSqlSessionFactory工厂对象
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(resourceAsStream);

    // 3.问题：openSession()执行逻辑是什么？
    // 3. (1)创建事务对象 （2）创建了执行器对象cachingExecutor (3)创建了DefaultSqlSession对象
    SqlSession sqlSession = sqlSessionFactory.openSession();

    // 4. 委派给Executor来执行，Executor执行时又会调用很多其他组件（参数设置、解析sql的获取，sql的执行、结果集的封装）
    User user = sqlSession.selectOne("com.itheima.mapper.UserMapper.findByCondition", 1);

    System.out.println(user);
    System.out.println("MyBatis源码环境搭建成功....");

    sqlSession.close();

  }


  /**
   * 通过mapper代理方式来完成一次查询操作
   * @throws IOException
   * 问题1：<package name="com.itheima.mapper"/> 是如何进行解析的？
   * 解答：解析得到name属性的值(包名)-->根据包名加载该包下所有的mapper接口--->将mapper接口及代理工厂对象存到knownMappers map集合中
   *      根据mapper接口的路径替换. / ---根据替换后路径定位到对应的映射配置文件--->XMLMapperBuilder.parse() 注解方式解析
   *
   * 问题2：sqlSession.getMapper(UserMapper.class); 是如何生成的代理对象？
   * 解答：(T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface }, mapperProxy);
   *       使用的就是JDK动态代理
   *
   * 问题3：mapperProxy.findByCondition(1); 是怎么完成的增删改查操作？
   * 解答：invoke()--->根据sqlCommandType值来判断是要进行增删改查那种操作--->查询：再判断返回值类型-->sqlSession里面的方法
   */
  @Test
  public void test2() throws IOException {

    // 1. 通过类加载器对配置文件进行加载，加载成了字节输入流，存到内存中 注意：配置文件并没有被解析
    InputStream resourceAsStream = Resources.getResourceAsStream("sqlMapConfig.xml");

    // 2. (1)解析了配置文件，封装configuration对象 （2）创建了DefaultSqlSessionFactory工厂对象
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(resourceAsStream);

    // 3.问题：openSession()执行逻辑是什么？
    // 3. (1)创建事务对象 （2）创建了执行器对象cachingExecutor (3)创建了DefaultSqlSession对象
    SqlSession sqlSession = sqlSessionFactory.openSession();

    // 4. JDK动态代理生成代理对象
    UserMapper mapperProxy = sqlSession.getMapper(UserMapper.class);

    // 5.代理对象调用方法
    User user = mapperProxy.findByCondition(1);
    User user2 = mapperProxy.findByCondition(1);

    System.out.println(user);
    System.out.println("MyBatis源码环境搭建成功....");

    sqlSession.close();

  }




}
