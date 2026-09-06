package com.temple.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TemplePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(TemplePlatformApplication.class, args);
    }
}
