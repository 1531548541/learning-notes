package com.mini.spring.ioc;

import com.mini.spring.beans.BeansException;
import com.mini.spring.beans.factory.config.BeanPostProcessor;

/**
 * @Author: wujie
 * @Date: 2024/7/14 21:04
 */
public class MyBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if ("userService".equals(beanName)) {
            UserServiceImpl userService = (UserServiceImpl) bean;
            userService.setLocation("改为：北京");
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
