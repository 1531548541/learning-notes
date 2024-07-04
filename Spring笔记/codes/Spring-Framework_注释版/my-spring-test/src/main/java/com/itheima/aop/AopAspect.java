package com.itheima.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Component
@Aspect // 切面
public class AopAspect {

	@Pointcut("execution(* com.itheima.service..*.*(..))")
	public void pointcut() {
	}

	@Before("pointcut()")
	public void before() {
		System.out.println("before");
	}

	@After("pointcut()")
	public void after() {
		System.out.println("after");
	}

	@Around("pointcut()")
	public Object around(ProceedingJoinPoint proceedingJoinPoint) throws InterruptedException {
		System.out.println("around advice start");
		try {
			Object result = proceedingJoinPoint.proceed();
			System.out.println("around advice end");
			return result;
		} catch (Throwable throwable) {
			throwable.printStackTrace();
			return null;
		}
	}

}
