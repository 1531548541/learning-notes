package com.mini.spring.ioc;

import com.mini.spring.beans.factory.config.BeanDefinition;
import com.mini.spring.beans.factory.BeanFactory;
import com.mini.spring.beans.factory.support.DefaultListableBeanFactory;
import org.junit.Test;

/**
 * @Author: wujie
 * @Date: 2024/7/10 21:24
 */
public class TestBeanFactory {

    @Test
    public void testBeanFactory() {
        //1.注册beanDefinition
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        String beanName = "userService";
        beanFactory.registerBeanDefinition(beanName,new BeanDefinition(UserServiceImpl.class));
        //2.getBean
        UserServiceImpl userService = (UserServiceImpl) beanFactory.getBean(beanName);
        userService.query();

        //第二次不会创建对象
        UserServiceImpl userService1 = (UserServiceImpl) beanFactory.getBean(beanName);
        userService1.query();
    }
}
