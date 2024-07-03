package com.itheima.chain;

import com.itheima.chain.impl.AspectJAfterAdvice;
import com.itheima.chain.impl.BeforeMethodInterceptor;
import com.itheima.chain.impl.ReflectiveMethodInvocation;

import java.util.ArrayList;
import java.util.List;

public class ChainTest {

	public static void main(String[] args) throws Throwable {

		// 执行器链
		List<MethodInterceptor> lists = new ArrayList<>();
		//后置增强
		AspectJAfterAdvice aspectJAfterAdvice = new AspectJAfterAdvice();
		//前置增强
		BeforeMethodInterceptor beforeMethodInterceptor = new BeforeMethodInterceptor();
		lists.add(beforeMethodInterceptor);
		lists.add(aspectJAfterAdvice);


		ReflectiveMethodInvocation reflectiveMethodInvocation = new ReflectiveMethodInvocation(lists);
		reflectiveMethodInvocation.proceed();

	}

}
