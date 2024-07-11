package com.mini.spring.beans.factory;

/**
 * @Author: wujie
 * @Date: 2024/7/10 21:17
 */
public interface BeanFactory {

    Object getBean(String beanName) throws BeansException;;
}
