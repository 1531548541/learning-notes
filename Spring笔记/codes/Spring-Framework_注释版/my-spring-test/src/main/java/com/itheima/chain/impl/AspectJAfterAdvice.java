package com.itheima.chain.impl;

import com.itheima.chain.MethodInterceptor;
import com.itheima.chain.MethodInvocation;

public class AspectJAfterAdvice implements MethodInterceptor {
	@Override
	public Object invoke(MethodInvocation mi) throws Throwable {

		Object var = null;
		try{
			var = mi.proceed();
		}finally {
			System.out.println("后置方法....");
		}
		return var;
	}
}
