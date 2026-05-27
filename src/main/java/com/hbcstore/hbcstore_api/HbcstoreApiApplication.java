package com.hbcstore.hbcstore_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HbcstoreApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(HbcstoreApiApplication.class, args);
	}

}
