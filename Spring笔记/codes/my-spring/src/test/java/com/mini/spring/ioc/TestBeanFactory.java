package com.mini.spring.ioc;

import com.mini.spring.beans.factory.config.BeanDefinition;
import com.mini.spring.beans.factory.BeanFactory;
import com.mini.spring.beans.factory.support.DefaultListableBeanFactory;
import com.mini.spring.beans.factory.support.SimpleInstantiationStrategy;
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
        //2.注册beanDefinition
        beanFactory.registerBeanDefinition(beanName,new BeanDefinition(UserServiceImpl.class));
        beanFactory.setInstantiationStrategy(new SimpleInstantiationStrategy());
        //3.getBean
        UserServiceImpl userService = (UserServiceImpl) beanFactory.getBean(beanName,"zzhdsb");
        userService.query();

        //第二次不会创建对象
        UserServiceImpl userService1 = (UserServiceImpl) beanFactory.getBean(beanName);
        userService1.query();
    }
}
