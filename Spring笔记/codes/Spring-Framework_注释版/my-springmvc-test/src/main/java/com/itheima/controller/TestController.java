package com.itheima.controller;

import com.itheima.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/test")
public class TestController {

	/* 问题1：Spring和SpringMVC整合使用时，会创建一个容器还是两个容器（父子容器？）
		 答：会创建两个容器（父子容器关系）
		     根据contextLoaderListener创建了根容器，并且管理service、dao、事务对象...这样的bean
	 *
	 * 问题2：DispatcherServlet初始化过程中做了什么？
	 	 答：init()--> 创建子容器，setparent(根容器)，解析springmvc.xml，生成了一些bean对象（controller,springmvc的组件）
		     重：保存地址和handlerMethod的映射关系（mappinglookup、urlLookup）
		     初始化9大组件的过程
	 *
	 * 问题3：请求的执行流程是怎么样的？
	 * 		  （1）怎么根据请求url找到controller里面的方法的？
	 			答：获取请求地址的uri,根据uri获取初始化过程中保存的映射关系（RequestMappingInfo）,根据RequestMappingInfo获取handlerMethod
	 * 		  （2）怎么设置的参数？
	            答：根据method里面的参数名称，通过request获取到参数值Object[], 反射调用目标方法handler01，invoke(bean,Object[]);
	 *        （3）怎么向model设置的值？
	            答：底层就是向request域中设置的值
	 *        （4）怎么完成的视图渲染及跳转？
	            答：通过视图解析器解析逻辑视图（拼接前缀后缀），进行请求转发
	 *
	 */

	@Autowired
	private TestService testService;


	@RequestMapping(value = "/handle01",method = RequestMethod.GET,headers = {})
	public String handle01(Integer id, String name, Model model){

		// 1.调用service方法
		testService.testService();
		System.out.println(id);
		System.out.println(name);

		// 2.model中存值
		model.addAttribute("name","子慕");
		return "success";
	}


	@RequestMapping("/handle02")
	@ResponseBody
	public String handle02() {
		return "Text...";
	}



}
