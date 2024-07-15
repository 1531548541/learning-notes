package com.mini.spring.beans.factory;

import com.mini.spring.beans.BeansException;

/**
 * @Author: wujie
 * @Date: 2024/7/10 21:17
 */
public interface BeanFactory {

    Object getBean(String beanName) throws BeansException;

    Object getBean(String beanName, Object... args) throws BeansException;

    <T> T getBean(String beanName, Class<T> clazz) throws BeansException;

}
