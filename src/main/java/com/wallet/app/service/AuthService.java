package com.wallet.app.service;

import com.wallet.app.entity.User;
import com.wallet.app.entity.Wallet;
import com.wallet.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender; 
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Random;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private JavaMailSender mailSender;

    public User registerUser(User user) {
        // Initialize the new wallet instance safely
        Wallet newWallet = new Wallet();
        newWallet.setBalance(BigDecimal.valueOf(1000.00)); // Baseline baseline bonus
        newWallet.setUser(user);
        
        // Form the bi-directional association
        user.setWallet(newWallet);
        
        return userRepo.save(user);
    }

    public String generateOTP() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    // 👈 @Async HAS BEEN COMMENTED OUT so the app waits for Google's exact response
    // @Async 
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("titumaalo@gmail.com");
            message.setTo(toEmail);
            message.setSubject("Secure Wallet Access Code - " + System.currentTimeMillis());
            message.setText("Your identity verification code is: " + otp + "\nValid for 5 minutes.");
            
            System.out.println("🚨 ATTEMPTING TO SEND EMAIL TO: " + toEmail);
            
            mailSender.send(message); // This is the line that actually talks to Google
            
            System.out.println("✅ EMAIL SUCCESSFULLY HANDED TO GOOGLE!");
            
        } catch (Exception e) {
            // 👈 This catches the network crash and prints the EXACT reason to the logs
            System.err.println("❌ CRITICAL MAIL FATAL ERROR:");
            e.printStackTrace(); 
        }
    }
}
