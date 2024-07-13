package com.mini.spring.beans.factory.support;

import com.mini.spring.beans.BeansException;
import com.mini.spring.beans.factory.config.BeanDefinition;

import java.lang.reflect.Constructor;

public class SimpleInstantiationStrategy implements InstantiationStrategy {
    @Override
    public Object instantiate(BeanDefinition beanDefinition, String beanName, Constructor constructor, Object[] args) {
        Object bean;
        try {
            if (null == constructor) {
                bean = beanDefinition.getBeanClass().newInstance();
            } else {
                bean = constructor.newInstance(args);
            }
        } catch (Exception e) {
            throw new BeansException(e.getMessage());
        }
        return bean;
    }
}
