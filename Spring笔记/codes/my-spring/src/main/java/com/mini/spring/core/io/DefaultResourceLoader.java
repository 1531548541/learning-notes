package com.mini.spring.core.io;

import cn.hutool.core.lang.Assert;
import com.mini.spring.utils.ClassUtils;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @Author: wujie
 * @Date: 2024/7/13 15:05
 */
public class DefaultResourceLoader implements ResourceLoader {
    @Override
    public Resource getResource(String location) {
        Assert.notNull(location, "Location must not be null");
        if (location.startsWith(CLASSPATH_URL_PREFIX)) {
            return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()));
        } else {
            try {
                URL url = new URL(location);
                return new UrlResource(url);
            } catch (MalformedURLException e) {
                return new FileSystemResource(location);
            }
        }
    }
}
