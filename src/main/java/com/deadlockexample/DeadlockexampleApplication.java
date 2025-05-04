package com.deadlockexample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class DeadlockexampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(DeadlockexampleApplication.class, args);
	}

}
