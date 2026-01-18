package com.example.paymentsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling
@SpringBootApplication
public class PaymentsystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentsystemApplication.class, args);
    }

}
