package com.medai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MedaiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedaiApplication.class, args);
    }
}
