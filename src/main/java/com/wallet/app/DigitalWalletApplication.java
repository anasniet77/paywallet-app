package com.wallet.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.awt.Desktop;
import java.net.URI;

@SpringBootApplication
public class DigitalWalletApplication {

    public static void main(String[] args) {
        // We change this from the default run command to allow Java to use Desktop features
        SpringApplication app = new SpringApplication(DigitalWalletApplication.class);
        app.setHeadless(false); 
        app.run(args);
    }

    // This listens for Spring Boot to finish starting up
    @EventListener({ApplicationReadyEvent.class})
    public void launchBrowser() {
        try {
            String url = "http://localhost:8080";
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                System.out.println("🌐 Browser launched automatically!");
            }
        } catch (Exception e) {
            System.out.println("Could not launch browser automatically. Please open http://localhost:8080");
        }
    }
}
