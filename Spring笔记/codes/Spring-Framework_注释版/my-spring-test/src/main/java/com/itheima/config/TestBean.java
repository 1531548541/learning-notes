package com.itheima.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.stereotype.Component;

public class TestBean implements BeanFactoryAware, BeanNameAware, InitializingBean, DisposableBean {


	private String name;

	private BeanFactory beanFactory;
	private String beanName;

	public TestBean() {
		System.err.println("[构造器] TestBean");
	}

	public void testBeanInit(){
		System.err.println("[init-method方法]");
	}

	public void print(){
		System.err.println("[print方法]");
		System.err.println("spring源码环境构建完成...");
	}

	public void setName(String name) {
		System.err.println("[注入属性] 注入属性name");
		this.name = name;
	}

	// 这是BeanFactoryAware接口方法
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		System.err.println("[BeanFactoryAware接口] 调用BeanFactoryAware.setBeanFactory()");
		this.beanFactory = beanFactory;
	}

	// 这是BeanNameAware接口方法
	@Override
	public void setBeanName(String name) {
		System.err.println("[BeanNameAware接口] 调用BeanNameAware.setBeanName()");
		this.beanName = name;

	}

	// 通过<bean>的destroy-method属性指定的初始化方法
	@Override
	public void destroy() throws Exception {
		System.err.println("[destroy-method] 调用<bean>的destroy-method属性指定的初始化方法");
	}

	// 通过<bean>的init-method属性指定的初始化方法
	 public void myInit() {
		  System.err.println("[init-method] 调用<bean>的init-method属性指定的初始化方法");
	}

	// 通过<bean>的destroy-method属性指定的初始化方法
	@Override
	public void afterPropertiesSet() throws Exception {
		System.err.println("[InitializingBean接口] 调用InitializingBean.afterPropertiesSet()");

	}
}
