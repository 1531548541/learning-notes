package com.mini.spring.beans.factory.support;

import com.mini.spring.core.io.Resource;
import com.mini.spring.core.io.ResourceLoader;

/**
 * @Author: wujie
 * @Date: 2024/7/13 15:11
 */
public interface BeanDefinitionReader {

    BeanDefinitionRegistry getRegistry();

    ResourceLoader getResourceLoader();

    void loadBeanDefinitions(Resource resource);

    void loadBeanDefinitions(Resource... resources);

    void loadBeanDefinitions(String location);
}
