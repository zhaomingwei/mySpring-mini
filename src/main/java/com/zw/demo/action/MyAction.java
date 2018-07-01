package com.zw.demo.action;


import com.zw.demo.services.IDemoService;
import com.zw.spring.annotation.Autowired;
import com.zw.spring.annotation.Controller;
import com.zw.spring.annotation.RequestMapping;

@Controller
public class MyAction {

		@Autowired
		IDemoService demoService;
	
		@RequestMapping("/index.html")
		public void query(){

		}
	
}
