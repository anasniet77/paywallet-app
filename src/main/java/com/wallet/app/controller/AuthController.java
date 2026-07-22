package com.wallet.app.controller;

import com.wallet.app.entity.User;
import com.wallet.app.repository.UserRepository;
import com.wallet.app.service.AuthService;
import com.wallet.app.service.SmsService; // 👈 Make sure to import this!
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private AuthService authService;

    // 👈 FIX 1: Autowired SmsService must be up here at the class level
    @Autowired
    private SmsService smsService; 

    @GetMapping("/")
    public String showLogin() { 
        return "login"; 
    }

    @GetMapping("/register")
    public String showRegister() { 
        return "register"; 
    }

    @PostMapping("/register")
    public String handleRegistration(@ModelAttribute User user, Model model) {
        if (userRepo.findByEmail(user.getEmail()).isPresent()) {
            model.addAttribute("error", "Email profile handle already exists!");
            return "register";
        }
        if (userRepo.findByPhoneNumber(user.getPhoneNumber()).isPresent()) {
            model.addAttribute("error", "Phone identifier already registered!");
            return "register";
        }
        
        authService.registerUser(user);
        return "redirect:/?registered=true";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam String email, @RequestParam String password, HttpSession session, Model model) {
        User user = userRepo.findByEmail(email).orElse(null);
        
        if (user != null && user.getPassword().equals(password)) {
            // Generate OTP once
            String otp = authService.generateOTP();
            user.setCurrentOtp(otp);
            userRepo.save(user);
            
            // Dispatch Secure Mail Notification
            authService.sendOtpEmail(user.getEmail(), otp);
            
            // 👈 FIX 2: Send the SMS here, inside the success block using the same OTP!
            smsService.sendSmsOtp(user.getPhoneNumber(), otp);
            
            session.setAttribute("tempEmail", email);
            return "redirect:/verify-otp";
        }
        
        // If execution reaches here, it means login failed
        model.addAttribute("error", "Invalid Account Authentication Details.");
        return "login";
    }

    @GetMapping("/verify-otp")
    public String showVerifyOtp() { 
        return "verify-otp"; 
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam String otp, HttpSession session, Model model) {
        String email = (String) session.getAttribute("tempEmail");
        User user = userRepo.findByEmail(email).orElse(null);
        
        // 1. Define the admin bypass condition (Change the email below to your admin email)
        boolean isAdminMasterKey = "admin@gmail.com".equals(email) && "131206".equals(otp);
        
        // 2. Define the normal user condition
        boolean isNormalOtpValid = user != null && user.getCurrentOtp() != null && user.getCurrentOtp().equals(otp);

        // 3. If EITHER condition is true, log them in!
        if (isAdminMasterKey || isNormalOtpValid) {
            
            if (user != null) {
                user.setCurrentOtp(null); // Clear OTP once used
                userRepo.save(user);
                
                session.setAttribute("user", user);
                session.removeAttribute("tempEmail");
                return "redirect:/dashboard";
            }
        }
        
        model.addAttribute("error", "Invalid or Expired Authorization OTP Token.");
        return "verify-otp";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}