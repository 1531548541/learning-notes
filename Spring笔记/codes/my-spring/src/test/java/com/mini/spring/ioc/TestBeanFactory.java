package com.mini.spring.ioc;

import com.mini.spring.BeanDefinition;
import com.mini.spring.BeanFactory;
import org.junit.Test;

/**
 * @Author: wujie
 * @Date: 2024/7/10 21:24
 */
public class TestBeanFactory {

    @Test
    public void testBeanFactory() {
        BeanFactory beanFactory = new BeanFactory();
        beanFactory.registerBeanDefinition("userService", new BeanDefinition(new UserServiceImpl()));
        UserServiceImpl userService = (UserServiceImpl) beanFactory.getBean("userService");
        userService.query();
    }
}
