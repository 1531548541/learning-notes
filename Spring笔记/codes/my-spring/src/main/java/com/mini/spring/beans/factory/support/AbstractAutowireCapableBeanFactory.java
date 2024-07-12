package com.mini.spring.beans.factory.support;

import cn.hutool.core.bean.BeanUtil;
import com.mini.spring.beans.BeansException;
import com.mini.spring.beans.PropertyValue;
import com.mini.spring.beans.PropertyValues;
import com.mini.spring.beans.factory.config.BeanDefinition;
import com.mini.spring.beans.factory.config.BeanReference;

import java.lang.reflect.Constructor;

public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory {

    private InstantiationStrategy instantiationStrategy = new CglibSubclassingInstantiationStrategy();

    public InstantiationStrategy getInstantiationStrategy() {
        return instantiationStrategy;
    }

    public void setInstantiationStrategy(InstantiationStrategy instantiationStrategy) {
        this.instantiationStrategy = instantiationStrategy;
    }

    @Override
    protected Object createBean(String beanName, BeanDefinition beanDefinition, Object[] args) throws BeansException {
        Object bean;
        try {
            bean = creatBeanInstance(beanName, beanDefinition, args);
            //注入属性
            applyBeanProperty(bean, beanName, beanDefinition);
        } catch (Exception e) {
            throw new BeansException("Instantiation of bean failed", e);
        }
        addSingletonBean(beanName, bean);
        return bean;
    }

    private void applyBeanProperty(Object bean, String beanName, BeanDefinition beanDefinition) {
        PropertyValues propertyValues = beanDefinition.getPropertyValues();
        for (PropertyValue propertyValue : propertyValues.getPropertyValues()) {
            String name = propertyValue.getPropertyName();
            Object value = propertyValue.getPropertyValue();
            if (value instanceof BeanReference) {
                BeanReference beanReference = (BeanReference) value;
                //*** 递归获取属性bean ***
                value = getBean(beanReference.getBeanName());
            }
            //调用hutool，属性填充
            BeanUtil.setFieldValue(bean, name, value);
        }
    }

    private Object creatBeanInstance(String beanName, BeanDefinition beanDefinition, Object[] args) throws BeansException {
        //获取Constructor
        Constructor[] declaredConstructors = beanDefinition.getBeanClass().getDeclaredConstructors();
        Constructor usedConstructor = null;
        for (Constructor constructor : declaredConstructors) {
            //仅简单用长度来匹配，实际还需要考虑参数类型
            if (null == args || constructor.getParameterTypes().length == args.length) {
                usedConstructor = constructor;
                break;
            }
        }
        return instantiationStrategy.instantiate(beanDefinition, beanName, usedConstructor, args);
    }
}
