package com.mini.spring.beans.factory.context.support;

import com.mini.spring.beans.factory.support.DefaultListableBeanFactory;
import com.mini.spring.beans.factory.xml.XmlBeanDefinitionReader;

/**
 * @Author: wujie
 * @Date: 2024/7/14 20:03
 */
public abstract class AbstractXmlApplicationContext extends AbstractRefreshableApplicationContext {

    @Override
    protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) {
        XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory, this);
        String[] configLocations = getConfigLocations();
        if (null != configLocations) {
            beanDefinitionReader.loadBeanDefinitions(configLocations);
        }
    }

    protected abstract String[] getConfigLocations();
}
