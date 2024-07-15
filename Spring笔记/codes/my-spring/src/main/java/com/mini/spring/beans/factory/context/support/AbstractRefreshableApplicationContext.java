package com.mini.spring.beans.factory.context.support;

import com.mini.spring.beans.factory.ConfigurableListableBeanFactory;
import com.mini.spring.beans.factory.support.DefaultListableBeanFactory;

/**
 * @Author: wujie
 * @Date: 2024/7/14 19:56
 */
public abstract class AbstractRefreshableApplicationContext extends AbstractApplicationContext {

    private DefaultListableBeanFactory beanFactory;

    @Override
    protected void refreshBeanFactory() {
        DefaultListableBeanFactory beanFactory = createBeanFactory();
        loadBeanDefinitions(beanFactory);
        this.beanFactory = beanFactory;
    }

    @Override
    protected ConfigurableListableBeanFactory getBeanFactory(){
        return this.beanFactory;
    }

    private DefaultListableBeanFactory createBeanFactory() {
        return new DefaultListableBeanFactory();
    }

    protected abstract void loadBeanDefinitions(DefaultListableBeanFactory beanFactory);

}
