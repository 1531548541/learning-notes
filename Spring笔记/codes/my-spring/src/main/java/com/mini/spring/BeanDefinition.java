package com.mini.spring;

/**
 * @Author: wujie
 * @Date: 2024/7/10 21:16
 */
public class BeanDefinition {
    private Object bean;

    public BeanDefinition(Object bean) {
        this.bean = bean;
    }

    public Object getBean() {
        return bean;
    }
}
