package com.itheima.mybatistest.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.itheima.mybatistest.io.Resources;
import com.itheima.mybatistest.pojo.Configuration;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/**
 * 解析sqlMapConfig.xml（数据源相关信息、mapper.xml配置路径）
 */
public class XMLConfigBuilder {

    private Configuration configuration = new Configuration();

    public XMLConfigBuilder() {
    }

    /**
     * 使用dom4j+xpath解析配置文件，封装Configuration对象
     */
    public Configuration parse(InputStream inputStream) throws DocumentException {
        //1.封装properties
        Properties properties = new Properties();
        Document document = new SAXReader().read(inputStream);
        Element rootElement = document.getRootElement();
        List<Element> list = rootElement.selectNodes("//property");
        for (Element element : list) {
            properties.setProperty(element.attributeValue("name"), element.attributeValue("value"));
        }
        //2.创建DataSource
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setDriverClassName(properties.getProperty("driverClassName"));
        druidDataSource.setUrl(properties.getProperty("url"));
        druidDataSource.setUsername(properties.getProperty("username"));
        druidDataSource.setPassword(properties.getProperty("password"));
        configuration.setDataSource(druidDataSource);

        //3.解析mapper.xml
        // <mapper resource="mapper/UserMapper.xml"></mapper>
        List<Element> mapperList = rootElement.selectNodes("//mapper");
        for (Element element : mapperList) {
            String mapperPath = element.attributeValue("resource");
            InputStream mapperInputStream = Resources.getResourceAsSteam(mapperPath);
            XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(configuration);
            xmlMapperBuilder.parse(mapperInputStream);
        }
        return configuration;
    }
}
