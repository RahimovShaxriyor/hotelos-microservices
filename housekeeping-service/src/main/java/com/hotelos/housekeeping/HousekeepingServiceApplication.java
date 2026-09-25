package com.hotelos.housekeeping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HousekeepingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(HousekeepingServiceApplication.class, args);
    }
}
