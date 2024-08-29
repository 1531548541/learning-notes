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
            operand.put("hosts", "[\"http://140.246.27.127:9200\"]");
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
                            + " cast(_MAP['evnt_time'] AS TIMESTAMP) AS \"evnt_time\", "
                            + " _MAP['alert_type'] AS \"alert_type\", "
                            + " _MAP['evnt_rel'] AS \"evnt_rel\", "
                            + " cast(_MAP['evnt_rel.name'] AS varchar(10)) AS \"evnt_rel_name\", "
                            + " cast(_MAP['evnt_rel.parent'] AS varchar(10)) AS \"evnt_rel_parent\", "
                            + " _MAP['my_obj'] AS \"my_obj\", "
                            + " cast(_MAP['my_obj.a'] AS varchar(10)) AS \"a\", "
                            + " _MAP['my_obj.c'] AS \"c\", "
                            + " cast(_MAP['_id'] AS varchar(20)) AS \"id\" "
                            + " from \"%s\".\"%s\"", indexName, indexName);

            ViewTableMacro macro =
                    ViewTable.viewMacro(rootSchema, sql, Collections.singletonList(indexName),
                            Arrays.asList(indexName, "enis"), false);
            rootSchema.add("enis", macro);
            statement = calciteConnection.createStatement();
            /**
             * 查询有告警和文件的数据
             */
            String ownJoinSql="select file.*,alert.alert_type,alert.md5 from enis AS file,enis AS alert where alert.file_md5 =file.md5 and file.evnt_rel='file' and alert.evnt_rel='alert'";
            String alertJoinFileSql="select file.*,alert.alert_type,alert.md5 from enis AS file left join enis AS alert on alert.file_md5 =file.md5 where file.evnt_rel='file' and alert.evnt_rel='alert'";
            String fileJoinAlertSql="select file.*,alert.alert_type,alert.md5 from enis AS alert left join enis AS file on alert.file_md5 =file.md5 where file.evnt_rel='file' and alert.evnt_rel='alert'";
            /**
             * 根据嵌套类型（obj）的字段在where条件中查询
             * join字段无法在where中条件查询: where evnt_rel_parent ='xxx' 查询不到值。【普通的obj可以】
             */
            String querySql="select * from enis where a = 'a'";
            /**
             * 查询2024-08-01 ~ 2024-08-15 时间内的文件数.
             *
             */
//            String groupBySql="select evnt_time from enis where evnt_time  BETWEEN '2024-08-01 10:00:00' AND '2024-08-31 10:00:00'";
            String groupBySql="select count(1) as num from enis where evnt_rel='file' group by md5";
            //join的字段须指定类型 cast as varchar|xxx
            resultSet = statement.executeQuery(groupBySql);
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
