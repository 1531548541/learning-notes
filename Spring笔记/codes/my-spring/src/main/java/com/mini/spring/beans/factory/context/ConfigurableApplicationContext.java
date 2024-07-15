package com.mini.spring.beans.factory.context;

import com.mini.spring.beans.BeansException;

/**
 * @Author: wujie
 * @Date: 2024/7/14 19:30
 */
public interface ConfigurableApplicationContext extends ApplicationContext {

    void refresh() throws BeansException;
}
