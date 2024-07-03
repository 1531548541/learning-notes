package com.itheima.cyclic;

public class TestService1_constructor {

	private TestService2_constructor service_2;

	public TestService1_constructor(TestService2_constructor service_2) {
		this.service_2 = service_2;
	}

	public void aTest(){
		System.out.println("testService1，注入了属性" + service_2 );
	}
}
