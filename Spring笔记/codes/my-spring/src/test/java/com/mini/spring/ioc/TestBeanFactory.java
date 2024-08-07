package com.mini.spring.ioc;

import com.mini.spring.beans.PropertyValue;
import com.mini.spring.beans.PropertyValues;
import com.mini.spring.beans.factory.config.BeanDefinition;
import com.mini.spring.beans.factory.BeanFactory;
import com.mini.spring.beans.factory.config.BeanReference;
import com.mini.spring.beans.factory.context.support.ClassPathXmlApplicationContext;
import com.mini.spring.beans.factory.support.DefaultListableBeanFactory;
import com.mini.spring.beans.factory.support.SimpleInstantiationStrategy;
import com.mini.spring.beans.factory.xml.XmlBeanDefinitionReader;
import org.junit.Test;

/**
 * @Author: wujie
 * @Date: 2024/7/10 21:24
 */
public class TestBeanFactory {

    @Test
    public void testBeanFactory() {
        //1.创建beanFactory
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        String beanName = "userService";
        //2.填充propertyValues
        PropertyValues propertyValues = new PropertyValues();
        propertyValues.addPropertyValue(new PropertyValue("name", "xx"));
        propertyValues.addPropertyValue(new PropertyValue("userDao", new BeanReference("userDao")));
        BeanDefinition beanDefinition = new BeanDefinition(UserServiceImpl.class, propertyValues);
        //2.注册beanDefinition
        beanFactory.registerBeanDefinition("userDao", new BeanDefinition(UserDao.class));
        beanFactory.registerBeanDefinition(beanName, beanDefinition);
        beanFactory.setInstantiationStrategy(new SimpleInstantiationStrategy());
        //3.getBean
        UserServiceImpl userService = (UserServiceImpl) beanFactory.getBean(beanName, "zzhdsb");
        userService.queryByName("xiaowang");
        userService.query();
    }

    @Test
    public void testBeanFactoryByXml() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.setInstantiationStrategy(new SimpleInstantiationStrategy());
        XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
        xmlBeanDefinitionReader.loadBeanDefinitions("classpath:beans.xml");
        String beanName = "userService";
        UserServiceImpl userService = beanFactory.getBean(beanName, UserServiceImpl.class);
        userService.queryByName("xiaowang");
        userService.query();
    }

    @Test
    public void testByContext(){
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:beans.xml");
        UserServiceImpl userService = applicationContext.getBean("userService", UserServiceImpl.class);
        userService.queryByName("xiaowang");
        userService.query();
    }
}
