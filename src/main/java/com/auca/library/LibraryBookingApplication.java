package com.auca.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories("com.auca.library.repository")
@EntityScan(basePackages = {"com.auca.library.model", "com.auca.library.converter"})
public class LibraryBookingApplication {

	public static void main(String[] args) {
		SpringApplication.run(LibraryBookingApplication.class, args);
	}

}
