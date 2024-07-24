package com.example.calcite.query;

import com.example.calcite.schema.JavaHrSchema;
import com.example.calcite.utils.CloseUtils;
import org.apache.calcite.adapter.java.ReflectiveSchema;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.SchemaPlus;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

public class QueryByJvmDemo {
    public static void main(String[] args) {
        Connection connection =null;
        CalciteConnection calciteConnection=null;
        Statement statement =null;
        ResultSet resultSet =null;
        try {
            Class.forName("org.apache.calcite.jdbc.Driver");
            Properties info = new Properties();
            info.setProperty("lex", "JAVA");
            connection = DriverManager.getConnection("jdbc:calcite:", info);
            calciteConnection = connection.unwrap(CalciteConnection.class);
            SchemaPlus rootSchema = calciteConnection.getRootSchema();
            /**
             * 注册一个对象作为 schema ，通过反射读取 JavaHrSchema 对象内部结构，将其属性 employee 和 department 作为表
             */
            rootSchema.add("hr", new ReflectiveSchema(new JavaHrSchema()));
            statement = calciteConnection.createStatement();
            resultSet = statement.executeQuery(
                    "select e.emp_id, e.name as emp_name, e.dept_no, d.name as dept_name "
                            + "from hr.employee as e "
                            + "left join hr.department as d on e.dept_no = d.dept_no");
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
        }finally {
            CloseUtils.closeAuto(resultSet);
            CloseUtils.closeAuto(statement);
            CloseUtils.closeAuto(calciteConnection);
            CloseUtils.closeAuto(connection);
        }
    }
}
