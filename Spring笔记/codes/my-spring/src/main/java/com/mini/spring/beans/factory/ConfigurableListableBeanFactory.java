package com.mini.spring.beans.factory;

import com.mini.spring.beans.BeansException;
import com.mini.spring.beans.factory.config.AutowireCapableBeanFactory;
import com.mini.spring.beans.factory.config.BeanDefinition;
import com.mini.spring.beans.factory.config.ConfigurableBeanFactory;

/**
 * @Author: wujie
 * @Date: 2024/7/14 19:42
 */
public interface ConfigurableListableBeanFactory extends ListableBeanFactory, AutowireCapableBeanFactory, ConfigurableBeanFactory {

    BeanDefinition getBeanDefinition(String beanName) throws BeansException;

    void preInstantiateSingletons() throws BeansException;
}
