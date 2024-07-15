package com.mini.spring.beans.factory.context.support;

import com.mini.spring.beans.BeansException;
import com.mini.spring.beans.factory.BeanFactory;
import com.mini.spring.beans.factory.ConfigurableListableBeanFactory;
import com.mini.spring.beans.factory.config.BeanFactoryPostProcessor;
import com.mini.spring.beans.factory.config.BeanPostProcessor;
import com.mini.spring.beans.factory.context.ConfigurableApplicationContext;
import com.mini.spring.core.io.DefaultResourceLoader;

import java.util.Map;

/**
 * @Author: wujie
 * @Date: 2024/7/14 19:32
 */
public abstract class AbstractApplicationContext extends DefaultResourceLoader implements ConfigurableApplicationContext {

    @Override
    public void refresh() throws BeansException {
        //1.创建beanFactory，并加载beanDefinition
        refreshBeanFactory();
        //2.获取beanFactory
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        //3.在bean实例化前，执行beanFactoryPostProcessor
        invokeBeanFactoryPostProcessors(beanFactory);
        //4.BeanPostProcessor需要先于其他bean实例化前注册
        registerBeanPostProcessor(beanFactory);
        //5.提前实例化单例bean对象
        beanFactory.preInstantiateSingletons();
    }

    protected abstract void refreshBeanFactory();

    protected abstract ConfigurableListableBeanFactory getBeanFactory();

    private void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
        Map<String, BeanFactoryPostProcessor> beanFactoryPostProcessorMap = beanFactory.getBeansOfType(BeanFactoryPostProcessor.class);
        for (BeanFactoryPostProcessor beanFactoryPostProcessor : beanFactoryPostProcessorMap.values()) {
            beanFactoryPostProcessor.postProcessBeanFactory(beanFactory);
        }
    }

    private void registerBeanPostProcessor(ConfigurableListableBeanFactory beanFactory) {
        Map<String, BeanPostProcessor> beanPostProcessorMap = beanFactory.getBeansOfType(BeanPostProcessor.class);
        for (BeanPostProcessor beanPostProcessor : beanPostProcessorMap.values()) {
            beanFactory.addBeanPostProcessor(beanPostProcessor);
        }
    }

    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
        return getBeanFactory().getBeansOfType(type);
    }

    @Override
    public String[] getBeanDefinitionNames() {
        return getBeanFactory().getBeanDefinitionNames();
    }

    @Override
    public Object getBean(String name) throws BeansException {
        return getBeanFactory().getBean(name);
    }

    @Override
    public Object getBean(String name, Object... args) throws BeansException {
        return getBeanFactory().getBean(name, args);
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
        return getBeanFactory().getBean(name, requiredType);
    }
}
