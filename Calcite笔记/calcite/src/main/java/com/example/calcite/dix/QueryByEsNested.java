package com.example.calcite.dix;

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

public class QueryByEsNested {
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
            final String sql =
                    String.format(Locale.ROOT, "select "
                            + " _MAP['file_name']  AS \"file_name\", "
                            + " _MAP['file_size']  AS \"file_size\", "
                            + " cast(_MAP['file_md5'] AS varchar(20)) AS \"file_md5\", "
                            + " cast(_MAP['md5'] AS varchar(20)) AS \"md5\", "
                            + " _MAP['evnt_time'] AS \"evnt_time\", "
                            + " _MAP['alert_type'] AS \"alert_type\", "
                            + " _MAP['evnt_rel'] AS \"evnt_rel\", "
                            + " cast(_MAP['evnt_rel.name'] AS varchar) AS \"evnt_rel_name\", "
                            + " cast(_MAP['evnt_rel.parent'] AS varchar(20)) AS \"evnt_rel_parent\", "
                            + " cast(_MAP['_id'] AS varchar(20)) AS \"id\" "
                            + " from \"%s\".\"%s\"", indexName, indexName);

            ViewTableMacro macro =
                    ViewTable.viewMacro(rootSchema, sql, Collections.singletonList(indexName),
                            Arrays.asList(indexName, "enis"), false);
            rootSchema.add("enis", macro);
            statement = calciteConnection.createStatement();
//            String alertJoinFileSql="select file.*,alert.alert_type,alert.md5 from enis AS file left join enis AS alert on alert.file_md5 =file.md5 where file.evnt_rel='file'";
            String fileJoinAlertSql="select file.*,alert.alert_type,alert.md5 from enis AS alert left join enis AS file on alert.file_md5 =file.md5 where alert.evnt_rel='alert'";
//            String querySql="select * from enis where evnt_rel='alert'";
            //虽可以join，但没有利用es evnt_rel，即dsl语句中的has_parent、has_child（性能？）。join的字段须指定类型 cast as varchar|xxx
            resultSet = statement.executeQuery(fileJoinAlertSql);
//            resultSet = statement.executeQuery("select file_md5 from enis where evnt_rel='file' group by file_md5");
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
