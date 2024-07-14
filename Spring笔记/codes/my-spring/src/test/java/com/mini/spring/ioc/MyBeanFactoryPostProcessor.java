package com.mini.spring.ioc;

import com.mini.spring.beans.BeansException;
import com.mini.spring.beans.PropertyValue;
import com.mini.spring.beans.factory.ConfigurableListableBeanFactory;
import com.mini.spring.beans.factory.config.BeanDefinition;
import com.mini.spring.beans.factory.config.BeanFactoryPostProcessor;

/**
 * @Author: wujie
 * @Date: 2024/7/14 21:03
 */
public class MyBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        BeanDefinition beanDefinition = beanFactory.getBeanDefinition("userService");
        beanDefinition.getPropertyValues().addPropertyValue(new PropertyValue("company", "改为：字节跳动"));
    }
}
