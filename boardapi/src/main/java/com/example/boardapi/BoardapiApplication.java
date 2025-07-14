package com.example.boardapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.example.boardapi.config.FileStorageProperties;

@EnableJpaAuditing
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(FileStorageProperties.class)
public class BoardapiApplication {

	public static void main(String[] args) {
		SpringApplication.run(BoardapiApplication.class, args);
	}

}
