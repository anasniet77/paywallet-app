package com.wallet.app.service;

import com.wallet.app.entity.User;
import com.wallet.app.entity.Wallet;
import com.wallet.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepo;

    // 👈 Pulls your new API key from the Railway Variables
    @Value("${BREVO_API_KEY}")
    private String apiKey;

    public User registerUser(User user) {
        // Initialize the new wallet instance safely
        Wallet newWallet = new Wallet();
        newWallet.setBalance(BigDecimal.valueOf(1000.00)); // Baseline bonus
        newWallet.setUser(user);
        
        // Form the bi-directional association
        user.setWallet(newWallet);
        
        return userRepo.save(user);
    }

    public String generateOTP() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    public void sendOtpEmail(String toEmail, String otp) {
        try {
            // 1. Build the JSON payload matching your exact original text
            String jsonPayload = String.format(
                "{" +
                "\"sender\":{\"name\":\"PayWallet App\",\"email\":\"titumaalo@gmail.com\"}," +
                "\"to\":[{\"email\":\"%s\"}]," +
                "\"subject\":\"Secure Wallet Access Code - %d\"," +
                "\"htmlContent\":\"<html><body><h2>Your identity verification code is: <strong>%s</strong></h2><p>Valid for 5 minutes.</p></body></html>\"" +
                "}", toEmail, System.currentTimeMillis(), otp);

            // 2. Create the HTTP POST Request over Port 443 (Bypasses Railway Firewall)
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                .header("accept", "application/json")
                .header("api-key", apiKey)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

            System.out.println("🚨 ATTEMPTING HTTP EMAIL TO: " + toEmail);
            
            // 3. Fire the request
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            // 4. Log the result clearly
            if (response.statusCode() == 201) {
                System.out.println("✅ EMAIL SUCCESSFULLY HANDED TO BREVO API!");
            } else {
                System.err.println("❌ API REJECTED REQUEST. Response: " + response.body());
            }
            
        } catch (Exception e) {
            System.err.println("❌ CRITICAL HTTP FATAL ERROR:");
            e.printStackTrace(); 
        }
    }
}
