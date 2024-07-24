package com.example.calcite;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.ResourceUtils;

import java.io.Console;
import java.util.Properties;

@SpringBootTest
class CalciteApplicationTests {

    @Test
    void contextLoads() {
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
        YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
        yamlPropertiesFactoryBean.setResources(resourceLoader.getResource(ResourceUtils.FILE_URL_PREFIX + "config/test.yml"));
        Properties properties = yamlPropertiesFactoryBean.getObject();
        System.out.println(properties);
    }

}
