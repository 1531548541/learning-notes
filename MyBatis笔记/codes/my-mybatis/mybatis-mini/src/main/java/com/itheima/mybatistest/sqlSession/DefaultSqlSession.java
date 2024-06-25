package com.itheima.mybatistest.sqlSession;

import com.itheima.mybatistest.executor.Executor;
import com.itheima.mybatistest.pojo.Configuration;
import com.itheima.mybatistest.pojo.MappedStatement;

import java.lang.reflect.*;
import java.util.List;

public class DefaultSqlSession implements SqlSession {
    private Configuration configuration;
    private Executor executor;

    public DefaultSqlSession(Configuration configuration, Executor executor) {
        this.configuration = configuration;
        this.executor = executor;
    }

    public <E> List<E> selectList(String statementId, Object param) throws Exception {
        //实际交给executor执行
        MappedStatement mappedStatement = configuration.getMappedStatementMap().get(statementId);
        return executor.query(configuration, mappedStatement, param);
    }

    public <T> T selectOne(String statementId, Object param) throws Exception {
        List<Object> list = this.selectList(statementId, param);
        if (list.size() == 1) {
            return (T) list.get(0);
        } else if (list.size() > 1) {
            throw new RuntimeException("返回结果过多");
        }
        return null;
    }

    public void close() {
        executor.close();
    }

    @Override
    public <T> T getMapper(Class<?> mapperClass) {
        // 使用JDK动态代理生成基于接口的代理对象
        Object proxy = Proxy.newProxyInstance(DefaultSqlSession.class.getClassLoader(), new Class[]{mapperClass}, new InvocationHandler() {

            /*
                Object：代理对象的引用，很少用
                Method：被调用的方法的字节码对象
                Object[]：调用的方法的参数
             */
            @Override
            public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                // 具体的逻辑 ：执行底层的JDBC
                // 通过调用sqlSession里面的方法来完成方法调用
                // 参数的准备：1.statementId: com.itheima.dao.IUserDao.findAll  2.param
                // 问题1：无法获取现有的statementId
                // findAll
                String methodName = method.getName();
                // com.itheima.dao.IUserDao
                String className = method.getDeclaringClass().getName();
                String statementId = className + "." + methodName;

                // 方法调用：问题2：要调用sqlSession中增删改查的什么方法呢？
                // 改造当前工程：sqlCommandType
                MappedStatement mappedStatement = configuration.getMappedStatementMap().get(statementId);
                // select  update delete insert
                String sqlCommandType = mappedStatement.getSqlCommandType();
                switch (sqlCommandType){
                    case "select":
                        // 执行查询方法调用
                        // 问题3：该调用selectList还是selectOne?
                        Type genericReturnType = method.getGenericReturnType();
                        // 判断是否实现了 泛型类型参数化
                        if(genericReturnType instanceof ParameterizedType){
                            if(objects != null) {
                                return selectList(statementId, objects[0]);
                            }
                            return  selectList(statementId, null);
                        }
                        return selectOne(statementId,objects[0]);

                    case "update":
                        // 执行更新方法调用
                        break;
                    case "delete":
                        // 执行delete方法调用
                        break;
                    case "insert":
                        // 执行insert方法调用
                        break;
                }
                return null;
            }
        });
        return (T) proxy;
    }
}
