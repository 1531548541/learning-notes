package com.mini.spring.beans.factory.support;

import com.mini.spring.beans.factory.config.BeanDefinition;

import java.lang.reflect.Constructor;

/**
 * 实例化对象策略
 */
public interface InstantiationStrategy {

    Object instantiate(BeanDefinition beanDefinition, String beanName, Constructor constructor, Object[] args);

}
