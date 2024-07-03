package com.itheima.test;

import com.itheima.service.UserService;
import com.itheima.service.UserServiceImpl;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class AOPTest {

	/**
	 * AOP效果演示
	 * @param args
	 *
	 *  问题1：<aop:aspectj-autoproxy /> @EnableAspectJAutoProxy 起到了什么作用？
	 *  作用：向容器中注册了AnnotationAwareAspectJAutoProxyCreator---》beanpostProcess -->after生成代理对象
	 *
	 *  问题2: AnnotationAwareAspectJAutoProxyCreator中的after方法怎么完成的代理对象的创建？
	 *
	 */
	public static void main(String[] args) {

		ClassPathXmlApplicationContext applicationContext =
				new ClassPathXmlApplicationContext("classpath:applicationContext-aop.xml");

		UserService userService = (UserService) applicationContext.getBean("userServiceImpl");
		userService.findAll();
	}
}
