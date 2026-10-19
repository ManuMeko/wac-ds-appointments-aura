package com.wacds.appointments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AppointmentsApiApplication {

    public static void main(String[] args) {

        SpringApplication.run(
            AppointmentsApiApplication.class,
            args
        );
    }
}