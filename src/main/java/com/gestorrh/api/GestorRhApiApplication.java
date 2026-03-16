package com.gestorrh.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GestorRhApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(GestorRhApiApplication.class, args);
	}

}
