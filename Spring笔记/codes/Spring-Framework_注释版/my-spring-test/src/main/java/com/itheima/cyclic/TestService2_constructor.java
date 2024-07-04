package com.itheima.cyclic;


public class TestService2_constructor {

	private TestService1_constructor service_1;

	public TestService2_constructor(TestService1_constructor service_1) {
		this.service_1 = service_1;
	}

	public void bTest(){
		System.out.println("testService2，注入了属性" + service_1 );
	}


}
