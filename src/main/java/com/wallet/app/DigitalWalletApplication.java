package com.wallet.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync; // 👈 1. Import this

@SpringBootApplication
@EnableAsync // 👈 2. Add this annotation here
public class DigitalWalletApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigitalWalletApplication.class, args);
    }
}
