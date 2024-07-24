package com.example.calcite.query;

import com.example.calcite.utils.CloseUtils;
import org.apache.calcite.adapter.elasticsearch.ElasticsearchSchema;
import org.apache.calcite.adapter.elasticsearch.ElasticsearchSchemaFactory;
import org.apache.calcite.adapter.jdbc.JdbcSchema;
import org.apache.calcite.config.CalciteConnectionProperty;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.impl.ViewTable;
import org.apache.calcite.schema.impl.ViewTableMacro;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class QueryByEsDemo {
    public static void main(String[] args) {
        Connection connection = null;
        CalciteConnection calciteConnection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            Properties properties = new Properties();
            //忽略大小写
            properties.setProperty(CalciteConnectionProperty.CASE_SENSITIVE.camelName(), "false");
            connection = DriverManager.getConnection("jdbc:calcite:", properties);
            calciteConnection = connection.unwrap(CalciteConnection.class);
            SchemaPlus rootSchema = calciteConnection.getRootSchema();
            String indexName = "my_index_1104";
            Map<String, Object> operand = new HashMap<>();
            operand.put("hosts", "[\"http://192.168.200.130:9200\"]");
//            operand.put("username", "elastic");
//            operand.put("password", "Njzf1984!(*$$");
            operand.put("index", indexName);
            rootSchema.add(indexName, new ElasticsearchSchemaFactory().create(rootSchema, indexName, operand));
            // es需要提前增加view
            final String viewSql =
                    String.format(Locale.ROOT, "select _MAP['brand'] AS \"brand\", "
                            + " _MAP['pt']  AS \"pt\", "
                            + " _MAP['name'] AS \"name\", "
                            + " _MAP['color'] AS \"color\", "
                            + " _MAP['price'] AS \"price\", "
                            + " _MAP['description'] AS \"description\", "
                            + " _MAP['_id'] AS \"id\" " // _id field is implicit
                            + " from \"%s\".\"%s\"", indexName, indexName);

            ViewTableMacro macro =
                    ViewTable.viewMacro(rootSchema, viewSql, Collections.singletonList(indexName),
                            Arrays.asList(indexName, "view"), false);
            rootSchema.add("view", macro);
            statement = calciteConnection.createStatement();
            resultSet = statement.executeQuery("select * from view");
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
