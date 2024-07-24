package com.example.calcite.query;

import com.example.calcite.utils.CloseUtils;
import org.apache.calcite.adapter.kafka.KafkaTableFactory;
import org.apache.calcite.config.CalciteConnectionProperty;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.SchemaPlus;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class QueryByKafkaDemo {
    public static void main(String[] args) {
        Connection connection = null;
        CalciteConnection calciteConnection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            Properties properties = new Properties();
            //忽略大小写
            properties.setProperty(CalciteConnectionProperty.CASE_SENSITIVE.camelName(), "false");
            properties.setProperty(CalciteConnectionProperty.TOPDOWN_OPT.camelName(), "false");
            connection = DriverManager.getConnection("jdbc:calcite:", properties);
            calciteConnection = connection.unwrap(CalciteConnection.class);
            SchemaPlus rootSchema = calciteConnection.getRootSchema();
            String topicName = "wj";
            Map<String, Object> operand = new HashMap<>();
            operand.put("bootstrap.servers", "http://192.168.200.130:9092");
            operand.put("topic.name", topicName);
//            operand.put("row.converter","com.example.calcite.query.KafkaRowConverterTest");
            HashMap<Object, Object> consumerConfig = new HashMap<>();
            consumerConfig.put("group.id", "1");
            consumerConfig.put("fetch.max.wait.ms", "100000");
            operand.put("consumer.params", consumerConfig);
            operand.put("consumer.cust", KafkaMockConsumer.class.getName());
            rootSchema.add(topicName, new KafkaTableFactory().create(rootSchema, topicName, operand, null));
            statement = calciteConnection.createStatement();
            resultSet = statement.executeQuery("select STREAM  * from \"wj\"");
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
