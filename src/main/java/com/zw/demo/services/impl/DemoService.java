package com.zw.demo.services.impl;

import com.zw.demo.services.IDemoService;
import com.zw.spring.annotation.Service;

@Service
public class DemoService implements IDemoService {

	public String get(String name) {
		return "My name is " + name;
	}

}
