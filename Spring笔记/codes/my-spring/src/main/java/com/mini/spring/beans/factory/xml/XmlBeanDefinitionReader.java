package com.mini.spring.beans.factory.xml;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.XmlUtil;
import com.mini.spring.beans.BeansException;
import com.mini.spring.beans.PropertyValue;
import com.mini.spring.beans.factory.config.BeanDefinition;
import com.mini.spring.beans.factory.config.BeanReference;
import com.mini.spring.beans.factory.support.AbstractBeanDefinitionReader;
import com.mini.spring.beans.factory.support.BeanDefinitionRegistry;
import com.mini.spring.core.io.Resource;
import com.mini.spring.core.io.ResourceLoader;
import com.sun.deploy.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.InputStream;

/**
 * @Author: wujie
 * @Date: 2024/7/13 15:18
 */
public class XmlBeanDefinitionReader extends AbstractBeanDefinitionReader {

    public XmlBeanDefinitionReader(BeanDefinitionRegistry beanDefinitionRegistry) {
        super(beanDefinitionRegistry);
    }

    public XmlBeanDefinitionReader(BeanDefinitionRegistry beanDefinitionRegistry, ResourceLoader resourceLoader) {
        super(beanDefinitionRegistry, resourceLoader);
    }

    @Override
    public void loadBeanDefinitions(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            doLoadBeanDefinitions(inputStream);
        } catch (IOException | ClassNotFoundException e) {
            throw new BeansException("IOException parsing XML document from " + resource, e);
        }
    }

    @Override
    public void loadBeanDefinitions(Resource... resources) {
        for (Resource resource : resources) {
            loadBeanDefinitions(resource);
        }
    }

    @Override
    public void loadBeanDefinitions(String location) {
        Resource resource = getResourceLoader().getResource(location);
        loadBeanDefinitions(resource);
    }

    private void doLoadBeanDefinitions(InputStream resource) throws ClassNotFoundException {
        Document doc = XmlUtil.readXML(resource);
        Element root = doc.getDocumentElement();
        NodeList childNodes = root.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (!(childNode instanceof Element)) continue;
            if (!"bean".equals(childNode.getNodeName())) continue;
            //解析bean标签
            Element bean = (Element) childNode;
            String id = bean.getAttribute("id");
            String name = bean.getAttribute("name");
            String className = bean.getAttribute("class");
            Class<?> clazz = Class.forName(className);
            //优先用id作为beanName
            String beanName = StrUtil.isNotBlank(id) ? id : name;
            //name也为空，则使用首字母小写的className
            if (StrUtil.isBlank(beanName)) {
                beanName = StrUtil.lowerFirst(clazz.getSimpleName());
            }

            //组装beanDefinition
            BeanDefinition beanDefinition = new BeanDefinition(clazz);
            //读取property
            for (int j = 0; j < bean.getChildNodes().getLength(); j++) {
                Node propertyNode = bean.getChildNodes().item(j);
                if (!(propertyNode instanceof Element)) continue;
                if (!"property".equals(propertyNode.getNodeName())) continue;
                Element property = (Element) propertyNode;
                //解析标签
                String propertyName = property.getAttribute("name");
                String value = property.getAttribute("value");
                String ref = property.getAttribute("ref");
                Object propertyValue = StrUtil.isNotBlank(ref) ? new BeanReference(ref) : value;
                beanDefinition.getPropertyValues().addPropertyValue(new PropertyValue(propertyName, propertyValue));
            }
            if (getRegistry().containsBeanDefinition(beanName)) {
                throw new BeansException("Duplicate beanName[" + beanName + "] is not allowed");
            }
            // 注册 BeanDefinition
            getRegistry().registerBeanDefinition(beanName, beanDefinition);
        }
    }
}
