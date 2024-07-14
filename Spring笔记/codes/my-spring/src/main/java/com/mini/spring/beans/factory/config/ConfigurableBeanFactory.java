package com.mini.spring.beans.factory.config;

import com.mini.spring.beans.factory.BeanFactory;

/**
 * @Author: wujie
 * @Date: 2024/7/14 19:18
 */
public interface ConfigurableBeanFactory extends BeanFactory, SingletonBeanRegistry {
    String SCOPE_SINGLETON = "singleton";

    String SCOPE_PROTOTYPE = "prototype";

    void addBeanPostProcessor(BeanPostProcessor beanPostProcessor);
}
