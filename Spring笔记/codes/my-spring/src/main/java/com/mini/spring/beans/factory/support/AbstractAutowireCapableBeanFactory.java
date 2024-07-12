package com.mini.spring.beans.factory.support;

import com.mini.spring.beans.factory.BeansException;
import com.mini.spring.beans.factory.config.BeanDefinition;

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
        } catch (Exception e) {
            throw new BeansException("Instantiation of bean failed", e);
        }
        addSingletonBean(beanName, bean);
        return bean;
    }

    private Object creatBeanInstance(String beanName, BeanDefinition beanDefinition, Object[] args) throws BeansException {
        //获取Constructor
        Constructor[] declaredConstructors = beanDefinition.getBeanClass().getDeclaredConstructors();
        Constructor usedConstructor = null;
        for (Constructor constructor : declaredConstructors) {
            //仅简单用长度来匹配，实际还需要考虑参数类型
            if (constructor.getParameterTypes().length == args.length) {
                usedConstructor = constructor;
                break;
            }
        }
        return instantiationStrategy.instantiate(beanDefinition, beanName, usedConstructor, args);
    }
}
