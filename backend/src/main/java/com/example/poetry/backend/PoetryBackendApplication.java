package com.example.poetry.backend;

import com.example.poetry.backend.common.config.AvatarStorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AvatarStorageProperties.class)
public class PoetryBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PoetryBackendApplication.class, args);
    }
}
