package com.itheima.chain;

public interface MethodInterceptor  {

	Object invoke(MethodInvocation mi) throws Throwable;

}
