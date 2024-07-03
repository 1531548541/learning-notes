package com.itheima.chain.impl;

import com.itheima.chain.MethodInterceptor;
import com.itheima.chain.MethodInvocation;

public class BeforeMethodInterceptor implements MethodInterceptor {

	@Override
	public Object invoke(MethodInvocation mi) throws Throwable {
		System.out.println("前置方法...");
		return mi.proceed();
	}
}
