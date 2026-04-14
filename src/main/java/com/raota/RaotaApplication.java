package com.raota;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RaotaApplication {
    static void main(String[] args) {
        SpringApplication.run(RaotaApplication.class, args);
    }
}
