package com.mini.spring.beans.factory.support;

import com.mini.spring.beans.factory.config.BeanDefinition;

public interface BeanDefinitionRegistry {

    /**
     * 向注册表中注册 BeanDefinition
     */
    void registerBeanDefinition(String beanName, BeanDefinition beanDefinition);

    /**
     * 判断是否包含指定名称的BeanDefinition
     */
    boolean containsBeanDefinition(String beanName);
}
