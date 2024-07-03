package com.itheima.cyclic;

public class TestService2 {

	private TestService1 testService1;

	public void setTestService1(TestService1 testService1) {
		this.testService1 = testService1;
	}

	public void aTest(){
		System.out.println("testService2，注入了属性" + testService1 );
	}
}
