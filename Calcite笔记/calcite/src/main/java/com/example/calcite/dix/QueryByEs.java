package com.example.calcite.dix;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.calcite.utils.CloseUtils;
import org.apache.calcite.adapter.elasticsearch.ElasticsearchSchemaFactory;
import org.apache.calcite.config.CalciteConnectionProperty;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.impl.ViewTable;
import org.apache.calcite.schema.impl.ViewTableMacro;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class QueryByEs {
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
            String indexName = "enis-evnt-2024.08";
            Map<String, Object> operand = new HashMap<>();
            operand.put("hosts", "[\"http://192.168.200.130:9200\"]");
//            operand.put("username", "elastic");
//            operand.put("password", "Njzf1984!(*$$");
            operand.put("index", indexName);
            rootSchema.add(indexName, new ElasticsearchSchemaFactory().create(rootSchema, indexName, operand));
            // es需要提前增加view
            final String fileSql =
                    String.format(Locale.ROOT, "select _MAP['file_name'] AS \"file_name\", "
                            + " _MAP['file_size']  AS \"file_size\", "
                            + " _MAP['file_md5'] AS \"file_md5\", "
                            + " _MAP['evnt_time'] AS \"evnt_time\", "
                            + " _MAP['evnt_rel'] AS \"evnt_rel\", "
                            + " _MAP['_id'] AS \"id\" "
                            + " from \"%s\".\"%s\"", indexName, indexName);

            final String alertSql =
                    String.format(Locale.ROOT, "select "
                            + " _MAP['alert_type'] AS \"alert_type\", "
                            + " _MAP['evnt_rel'] AS \"evnt_rel\", "
                            + " _MAP['evnt_rel.name'] AS \"evnt_rel_name\", "
                            + " _MAP['evnt_rel.parent'] AS \"parent\", "
                            + " _MAP['_id'] AS \"id\" "
                            + " from \"%s\".\"%s\"", indexName, indexName);

            ViewTableMacro fileMacro =
                    ViewTable.viewMacro(rootSchema, fileSql, Collections.singletonList(indexName),
                            Arrays.asList(indexName, "file"), false);
            rootSchema.add("file", fileMacro);
            ViewTableMacro alertMacro =
                    ViewTable.viewMacro(rootSchema, alertSql, Collections.singletonList(indexName),
                            Arrays.asList(indexName, "alert"), false);
            rootSchema.add("alert", alertMacro);
            statement = calciteConnection.createStatement();
//            resultSet = statement.executeQuery("select * from alert");
//            resultSet = statement.executeQuery("select * from file");
            resultSet = statement.executeQuery("select alert.*,file.* from alert,file where file.evnt_rel='file' and alert.id=file.id");
            /**
             * 遍历 SQL 执行结果
             */
            JSONArray res=new JSONArray();
            while (resultSet.next()) {
                JSONObject jsonObject = new JSONObject();
                for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                    jsonObject.put(resultSet.getMetaData().getColumnName(i) , resultSet.getObject(i));
                }
                res.add(jsonObject);
            }
            System.out.println(String.format("res有 %s 条，内容为: %s",res.size(),res));
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
