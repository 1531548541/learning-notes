package com.itheima.mybatistest.config;

import com.itheima.mybatistest.pojo.Configuration;
import com.itheima.mybatistest.pojo.MappedStatement;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 解析mapper.xml
 */
public class XMLMapperBuilder {

    private Configuration configuration;

    public XMLMapperBuilder(Configuration configuration) {
        this.configuration = configuration;
    }

    public Configuration parse(InputStream inputStream) throws DocumentException {
        Map<String, MappedStatement> mappedStatementMap = new HashMap<>();
        Document document = new SAXReader().read(inputStream);
        Element rootElement = document.getRootElement();
        String nameSpace = rootElement.attributeValue("namespace");
        List<Element> selectElementList = rootElement.selectNodes("//select");
        for (Element element : selectElementList) {
            MappedStatement mappedStatement = new MappedStatement();
            String id = element.attributeValue("id");
            String parameterType = element.attributeValue("parameterType");
            String resultType = element.attributeValue("resultType");
            String sql = element.getTextTrim();
            String statementId = nameSpace + "." + id;
            mappedStatement.setStatementId(statementId);
            mappedStatement.setParameterType(parameterType);
            mappedStatement.setResultType(resultType);
            mappedStatement.setSql(sql);
            mappedStatement.setSqlCommandType("select");
            mappedStatementMap.put(statementId, mappedStatement);
        }
        configuration.setMappedStatementMap(mappedStatementMap);
        return configuration;
    }
}
