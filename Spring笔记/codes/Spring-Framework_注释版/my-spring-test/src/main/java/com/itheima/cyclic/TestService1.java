package com.itheima.cyclic;

public class TestService1 {

	private TestService2 testService2;

	public void setTestService2(TestService2 testService2) {
		this.testService2 = testService2;
	}

	public void aTest(){
		System.out.println("testService1，注入了" + testService2 );
	}


}
