package com.itheima.chain.impl;

import com.itheima.chain.MethodInterceptor;
import com.itheima.chain.MethodInvocation;

import java.util.List;

public class ReflectiveMethodInvocation implements MethodInvocation {

	List<MethodInterceptor> methodInterceptors;

	public ReflectiveMethodInvocation(List<MethodInterceptor> methodInterceptors) {
		this.methodInterceptors = methodInterceptors;
	}

	private  int index = -1;

	@Override
	public Object proceed() throws Throwable {
		Object var = null;
		if(index == this.methodInterceptors.size()-1){
			System.out.println("调用被代理的原本方法");
		}else{
			methodInterceptors.get(++index).invoke(this);
		}
		return var;
	}
}
