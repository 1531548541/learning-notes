package com.mini.spring.core.io;

/**
 * Resource加载器
 * @Author: wujie
 * @Date: 2024/7/13 15:04
 */
public interface ResourceLoader {
    String CLASSPATH_URL_PREFIX = "classpath:";

    Resource getResource(String location);
}
