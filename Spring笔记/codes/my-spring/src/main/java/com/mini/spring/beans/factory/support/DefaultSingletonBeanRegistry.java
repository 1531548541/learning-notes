package com.mini.spring.beans.factory.support;

import com.mini.spring.beans.factory.config.SingletonBeanRegistry;

import java.util.HashMap;
import java.util.Map;

public class DefaultSingletonBeanRegistry implements SingletonBeanRegistry {

    private Map<String, Object> singleObjects = new HashMap<>();

    @Override
    public Object getSingletonBean(String beanName) {
        return singleObjects.get(beanName);
    }

    protected void addSingletonBean(String beanName, Object singleObject) {
        singleObjects.put(beanName, singleObject);
    }
}
