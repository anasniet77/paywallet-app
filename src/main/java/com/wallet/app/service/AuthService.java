package com.wallet.app.service;

import com.wallet.app.entity.User;
import com.wallet.app.entity.Wallet;
import com.wallet.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender; // Explicitly fixed import path
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

    public void sendOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("your_actual_gmail@gmail.com");
            message.setTo(toEmail);
            message.setSubject("Secure Wallet Access Code - " + System.currentTimeMillis());
            message.setText("Your identity verification code is: " + otp + "\nValid for 5 minutes.");
            mailSender.send(message);
        } catch (Exception e) {
            // This catches the network crash and prints it silently to the console 
            // instead of rolling back your user registrations!
            System.out.println("⚠️ SMTP Network Error: OTP code [" + otp + "] could not be emailed. Printing to Eclipse console instead!");
        }
    }
}