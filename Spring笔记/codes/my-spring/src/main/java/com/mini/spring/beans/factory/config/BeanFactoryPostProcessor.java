package com.mini.spring.beans.factory.config;

import com.mini.spring.beans.BeansException;
import com.mini.spring.beans.factory.ConfigurableListableBeanFactory;

/**
 * 在所有的 BeanDefinition 加载完成后，实例化 Bean 对象之前，提供修改 BeanDefinition 属性的机制
 * @Author: wujie
 * @Date: 2024/7/14 19:22
 */
public interface BeanFactoryPostProcessor {

    /**
     * 在所有的 BeanDefinition 加载完成后，实例化 Bean 对象之前，提供修改 BeanDefinition 属性的机制
     *
     * @param beanFactory
     * @throws BeansException
     */
    void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;

}
