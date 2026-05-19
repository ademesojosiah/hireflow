package com.hireflow.hireflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class HireflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(HireflowApplication.class, args);
	}

}
