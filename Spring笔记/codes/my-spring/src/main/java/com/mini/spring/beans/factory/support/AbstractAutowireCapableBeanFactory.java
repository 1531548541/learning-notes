package com.mini.spring.beans.factory.support;

import cn.hutool.core.bean.BeanUtil;
import com.mini.spring.beans.BeansException;
import com.mini.spring.beans.PropertyValue;
import com.mini.spring.beans.PropertyValues;
import com.mini.spring.beans.factory.config.AutowireCapableBeanFactory;
import com.mini.spring.beans.factory.config.BeanDefinition;
import com.mini.spring.beans.factory.config.BeanPostProcessor;
import com.mini.spring.beans.factory.config.BeanReference;

import java.lang.reflect.Constructor;
import java.util.List;

public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory implements AutowireCapableBeanFactory {

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
            applyBeanProperty(beanName, bean, beanDefinition);
            //执行 Bean 的初始化方法和 BeanPostProcessor 的前置和后置处理方法
            bean = initializeBean(beanName, bean, beanDefinition);
        } catch (Exception e) {
            throw new BeansException("Instantiation of bean failed", e);
        }
        addSingletonBean(beanName, bean);
        return bean;
    }

    private Object initializeBean(String beanName, Object bean, BeanDefinition beanDefinition) {
        //1.执行BeanPostProcessor Before 处理
        Object wrappedBean = applyBeanPostProcessorsBeforeInitialization(bean, beanName);
        //2.初始化bean
        invokeInitMethods(beanName, wrappedBean, beanDefinition);
        //3.执行 BeanPostProcessor After 处理
        wrappedBean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
        return wrappedBean;
    }

    private void applyBeanProperty(String beanName, Object bean, BeanDefinition beanDefinition) {
        try {
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
        } catch (Exception e) {
            throw new BeansException("Error setting property values：" + beanName);
        }
    }

    private Object creatBeanInstance(String beanName, BeanDefinition beanDefinition, Object[] args) throws BeansException {
        //获取Constructor
        Constructor[] declaredConstructors = beanDefinition.getBeanClass().getDeclaredConstructors();
        Constructor usedConstructor = null;
        for (Constructor constructor : declaredConstructors) {
            //仅简单用长度来匹配，实际还需要考虑参数类型
            if (null != args && constructor.getParameterTypes().length == args.length) {
                usedConstructor = constructor;
                break;
            }
        }
        return instantiationStrategy.instantiate(beanDefinition, beanName, usedConstructor, args);
    }

    @Override
    public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName) throws BeansException {
        Object result = existingBean;
        List<BeanPostProcessor> beanPostProcessors = getBeanPostProcessors();
        for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
            Object current = beanPostProcessor.postProcessBeforeInitialization(existingBean, beanName);
            if (null == current) {
                return result;
            }
            result = current;
        }
        return result;
    }

    @Override
    public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName) throws BeansException {
        Object result = existingBean;
        for (BeanPostProcessor processor : getBeanPostProcessors()) {
            Object current = processor.postProcessAfterInitialization(result, beanName);
            if (null == current) return result;
            result = current;
        }
        return result;
    }

    //TODO
    private void invokeInitMethods(String beanName, Object wrappedBean, BeanDefinition beanDefinition) {

    }
}
