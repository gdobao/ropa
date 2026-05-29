package com.colorinchi.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;

@EnableCaching
@EnableRetry
@SpringBootApplication
@ConfigurationPropertiesScan
public class ColorinchiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ColorinchiApplication.class, args);
    }
}
