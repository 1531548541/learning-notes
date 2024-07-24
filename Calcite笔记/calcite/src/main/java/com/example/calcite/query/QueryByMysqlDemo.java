package com.example.calcite.query;

import com.example.calcite.utils.CloseUtils;
import org.apache.calcite.adapter.jdbc.JdbcSchema;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.SchemaPlus;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class QueryByMysqlDemo {
    public static void main(String[] args) {
        Connection connection = null;
        CalciteConnection calciteConnection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            Properties properties = new Properties();
            properties.put("lex", "MYSQL");
            connection = DriverManager.getConnection("jdbc:calcite:", properties);
            calciteConnection = connection.unwrap(CalciteConnection.class);
            SchemaPlus rootSchema = calciteConnection.getRootSchema();
            Map<String, Object> operand = new HashMap<>();
            operand.put("jdbcUrl", "jdbc:mysql://localhost:3306/company_project");
            operand.put("jdbcDriver", "com.mysql.jdbc.Driver");
            operand.put("jdbcUser", "root");
            operand.put("jdbcPassword", "123456");
            JdbcSchema jdbcSchema = JdbcSchema.create(rootSchema, "company_project", operand);
            rootSchema.add("company_project", jdbcSchema);
            statement = connection.createStatement();
            resultSet = statement.executeQuery("select * from company_project.t_slslog");
            /**
             * 遍历 SQL 执行结果
             */
            while (resultSet.next()) {
                for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                    System.out.print(resultSet.getMetaData().getColumnName(i) + ":" + resultSet.getObject(i));
                    System.out.print(" | ");
                }
                System.out.println();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CloseUtils.closeAuto(resultSet);
            CloseUtils.closeAuto(statement);
            CloseUtils.closeAuto(calciteConnection);
            CloseUtils.closeAuto(connection);
        }
    }
}
