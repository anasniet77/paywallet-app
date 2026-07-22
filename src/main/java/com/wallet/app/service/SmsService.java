package com.wallet.app.service;
//745DNE5S1JGPPBU7TJ4TQB1D
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async; // 👈 1. Import added here
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String twilioPhoneNumber;

    // Initializes the Twilio client as soon as Spring Boot starts
    @PostConstruct
    public void initTwilio() {
        Twilio.init(accountSid, authToken);
    }

    @Async // 👈 2. Added this so Twilio runs in the background
    public void sendSmsOtp(String targetPhoneNumber, String otpCode) {
        try {
            // Ensure phone numbers include country code (e.g., +91 for India)
            if (!targetPhoneNumber.startsWith("+")) {
                targetPhoneNumber = "+91" + targetPhoneNumber; 
            }

            Message message = Message.creator(
                    new PhoneNumber(targetPhoneNumber), // To
                    new PhoneNumber(twilioPhoneNumber), // From
                    "Your Digital Wallet verification code is: " + otpCode + ". Do not share this with anyone."
            ).create();

            System.out.println("SMS sent successfully! Message SID: " + message.getSid());
            
        } catch (Exception e) {
            System.err.println("Failed to send SMS: " + e.getMessage());
        }
    }
}
