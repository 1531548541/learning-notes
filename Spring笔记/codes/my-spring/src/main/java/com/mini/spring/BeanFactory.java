package com.mini.spring;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: wujie
 * @Date: 2024/7/10 21:17
 */
public class BeanFactory {
    private Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();

    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition){
        beanDefinitionMap.put(beanName, beanDefinition);
    }

    public Object getBean(String beanName){
        return beanDefinitionMap.get(beanName).getBean();
    }
}
