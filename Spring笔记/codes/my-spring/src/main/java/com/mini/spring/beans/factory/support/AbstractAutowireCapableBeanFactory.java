package com.mini.spring.beans.factory.support;

import com.mini.spring.beans.factory.BeansException;
import com.mini.spring.beans.factory.config.BeanDefinition;

public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory {
    @Override
    protected Object createBean(String beanName, BeanDefinition beanDefinition) throws BeansException {
        Object bean;
        try {
            bean = beanDefinition.getBeanClass().newInstance();
        } catch (Exception e) {
            throw new BeansException("Instantiation of bean failed", e);
        }
        addSingletonBean(beanName, bean);
        return bean;
    }
}
