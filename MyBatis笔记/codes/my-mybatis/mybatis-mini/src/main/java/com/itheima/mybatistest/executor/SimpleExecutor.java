package com.itheima.mybatistest.executor;

import com.itheima.mybatistest.config.BoundSql;
import com.itheima.mybatistest.pojo.Configuration;
import com.itheima.mybatistest.pojo.MappedStatement;
import com.itheima.mybatistest.utils.GenericTokenParser;
import com.itheima.mybatistest.utils.ParameterMapping;
import com.itheima.mybatistest.utils.ParameterMappingTokenHandler;
import org.apache.commons.lang3.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * query：
 * 1.获得connection
 * 2.拼sql
 * 3.设置预编译参数
 * 4.调用jdbc的query
 * 5.对resultSet进行映射
 */
public class SimpleExecutor implements Executor {

    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;

    public <E> List<E> query(Configuration configuration, MappedStatement mappedStatement, Object param) throws Exception {
        List<E> resultList=new ArrayList<>();
        // 1.获得connection
        connection = configuration.getDataSource().getConnection();
        // 2.拼sql
        String sql = mappedStatement.getSql();
        BoundSql boundSql = getBoundSql(sql);
        preparedStatement = connection.prepareStatement(boundSql.getFinalSql());
        // 3.设置预编译参数
        String parameterType = mappedStatement.getParameterType();
        if (StringUtils.isNotBlank(parameterType)) {
            Class<?> parameterTypeClass = Class.forName(parameterType);
            List<ParameterMapping> parameterMappingList = boundSql.getParameterMappingList();
            for (int i = 0; i < parameterMappingList.size(); i++) {
                ParameterMapping parameterMapping = parameterMappingList.get(i);
                String paramName = parameterMapping.getContent();
                //反射获取field
                Field field = parameterTypeClass.getDeclaredField(paramName);
                field.setAccessible(true);
                Object value = field.get(param);
                preparedStatement.setObject(i + 1, value);
            }
        }
        // 4.调用jdbc的query
        resultSet = preparedStatement.executeQuery();
        // 5.对resultSet进行映射
        String resultType = mappedStatement.getResultType();
        Class<?> resultTypeClass = Class.forName(resultType);
        while (resultSet.next()) {
            // 元数据信息 包含了 字段名  字段的值
            ResultSetMetaData metaData = resultSet.getMetaData();
            Object result = resultTypeClass.newInstance();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String columnName = metaData.getColumnName(i);
                Object columnValue = resultSet.getObject(columnName);
                // 问题：现在要封装到哪一个实体中
                // 封装
                // 属性描述器：通过API方法获取某个属性的读写方法
                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(columnName, resultTypeClass);
                Method writeMethod = propertyDescriptor.getWriteMethod();
                writeMethod.invoke(result, columnValue);
            }
            resultList.add((E) result);
        }
        return resultList;
    }

    /**
     * 1.#{}占位符替换成？
     * 2.解析替换的过程中 将#{}里面的值保存下来
     */
    private BoundSql getBoundSql(String sql) {
        // 1.创建标记处理器：配合标记解析器完成标记的处理解析工作
        ParameterMappingTokenHandler parameterMappingTokenHandler = new ParameterMappingTokenHandler();
        // 2.创建标记解析器
        GenericTokenParser genericTokenParser = new GenericTokenParser("#{", "}", parameterMappingTokenHandler);

        // #{}占位符替换成？ 2.解析替换的过程中 将#{}里面的值保存下来 ParameterMapping
        String finalSql = genericTokenParser.parse(sql);

        // #{}里面的值的一个集合 id username
        List<ParameterMapping> parameterMappings = parameterMappingTokenHandler.getParameterMappings();

        BoundSql boundSql = new BoundSql(finalSql, parameterMappings);

        return boundSql;
    }

    public void close() {
        // 释放资源
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (preparedStatement != null) {
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
