package com.wallet.app.controller;

import com.wallet.app.entity.LinkedBankAccount;
import com.wallet.app.entity.User;
import com.wallet.app.entity.Wallet;
import com.wallet.app.repository.UserRepository;
import com.wallet.app.repository.WalletRepository;
import com.wallet.app.service.WalletService;

// Razorpay Imports
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/dashboard")
public class WalletController {
    
    @Autowired
    private com.wallet.app.repository.LinkedBankAccountRepository bankRepo;
    
    @Autowired
    private WalletService walletService;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private WalletRepository walletRepo; 

    // --- RAZORPAY CREDENTIALS ---
    @Value("${razorpay.key.id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret:}")
    private String razorpayKeySecret;

    // --- MAIN DASHBOARD ---
    @GetMapping
    public String showDashboard(HttpSession session, Model model) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) return "redirect:/";

        User user = userRepo.findById(sessionUser.getId()).orElse(null);
        if (user == null) return "redirect:/logout";

        Wallet wallet = walletRepo.findByUserId(user.getId()); 
        model.addAttribute("wallet", wallet); 
        model.addAttribute("user", user);
        
        String txSuccess = (String) session.getAttribute("txSuccess");
        String txError = (String) session.getAttribute("txError");
        if (txSuccess != null) { model.addAttribute("txSuccess", txSuccess); session.removeAttribute("txSuccess"); }
        if (txError != null) { model.addAttribute("txError", txError); session.removeAttribute("txError"); }
        
        return "dashboard";
    }

    // --- NAVIGATION ROUTES ---
    @GetMapping("/profile")
    public String showProfilePage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/";
        model.addAttribute("user", user);
        return "profile";
    }

    @GetMapping("/security")
    public String showSecurityPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/";
        model.addAttribute("user", user);
        return "security";
    }

    @GetMapping("/settings")
    public String showSettingsPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/";
        model.addAttribute("user", user);
        return "settings";
    }

    @GetMapping("/send-money")
    public String showSendMoneyPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/";
        model.addAttribute("user", user);
        return "send-money"; 
    }

    @GetMapping("/add-funds")
    public String showAddFundsPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/";
        model.addAttribute("linkedBanks", bankRepo.findByUserId(user.getId()));
        model.addAttribute("user", user);
        return "add-funds";
    }

    @GetMapping("/link-bank")
    public String showLinkBankPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/";
        model.addAttribute("user", user);
        return "link-bank";
    }

    @GetMapping("/banks")
    public String showBankAccountsPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/";

        List<LinkedBankAccount> linkedBanks = bankRepo.findByUserId(user.getId());
        model.addAttribute("linkedBanks", linkedBanks);
        return "banks"; 
    }

    @GetMapping("/history")
    public String showHistoryPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/";
        model.addAttribute("transactions", walletService.fetchTransactionHistory(user.getPhoneNumber()));
        model.addAttribute("user", user);
        return "history";
    }
    
    // --- POST ACTIONS ---
    @PostMapping("/transfer")
    public String transferMoney(
            @RequestParam String receiverPhone, 
            @RequestParam BigDecimal amount, 
            @RequestParam String pin, // <-- NEW: Require PIN from the HTML form
            HttpSession session) {
            
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) return "redirect:/";
        
        User user = userRepo.findById(sessionUser.getId()).orElse(null);

        // SECURITY CHECK: Does the entered PIN match the database?
        if (user == null || user.getPin() == null || !user.getPin().equals(pin)) {
            session.setAttribute("txError", "Transaction Failed: Incorrect Security PIN.");
            return "redirect:/dashboard"; // or wherever your transfer page is
        }

        // If PIN is correct, process the transfer
        boolean success = walletService.sendMoney(user.getPhoneNumber(), receiverPhone, amount);
        if (success) {
            session.setAttribute("txSuccess", "Transaction Complete!");
        } else {
            session.setAttribute("txError", "Transaction Failed: Insufficient balance or invalid user.");
        }
        return "redirect:/dashboard";
    }
    
    @PostMapping("/link-bank")
    public String linkBankAccount(@RequestParam String bankName, @RequestParam String accountNumber, @RequestParam String ifscCode, HttpSession session) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) return "redirect:/";
        LinkedBankAccount bankAccount = new LinkedBankAccount();
        bankAccount.setBankName(bankName);
        bankAccount.setAccountNumber(accountNumber);
        bankAccount.setIfscCode(ifscCode.toUpperCase()); 
        bankAccount.setUser(sessionUser);
        bankRepo.save(bankAccount);
        session.setAttribute("txSuccess", "Bank linked successfully!");
        return "redirect:/dashboard";
    }

    @PostMapping("/change-pin")
    public String updateSecurityPin(@RequestParam String newPin, @RequestParam String otp, HttpSession session) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) return "redirect:/";
        
        User user = userRepo.findById(sessionUser.getId()).orElse(null);
        
        // Verify OTP matches what we saved in the database
        if (user != null && otp.equals(user.getCurrentOtp()) && newPin.length() == 4) {
            user.setPin(newPin); // Save the new PIN
            user.setCurrentOtp(null); // Clear the OTP so it can't be reused
            userRepo.save(user);
            session.setAttribute("txSuccess", "Security PIN updated successfully!");
        } else {
            session.setAttribute("txError", "Invalid OTP or PIN format.");
        }
        return "redirect:/dashboard/security";
    }

    @PostMapping("/delete-account")
    public String deleteUserAccount(HttpSession session) {
        session.invalidate(); 
        return "redirect:/?deleted=true";
    }

    // --- RAZORPAY INTEGRATION ENDPOINTS ---
    
    @PostMapping(value = "/create-order", produces = "application/json")
    @ResponseBody
    public String createRazorpayOrder(@RequestParam("amount") BigDecimal amount) {
        try {
            // 1. HARDCODING THE KEYS DIRECTLY TO BYPASS SPRING CACHE
            String myKeyId = "rzp_test_TGEgZXObLrLXH4";       
            String myKeySecret = "QHTdjJxQ4sxRIjaVB0BW6Dt3";   

            System.out.println("Attempting to connect to Razorpay with Key: " + myKeyId);

            // 2. Using the hardcoded keys
            RazorpayClient razorpay = new RazorpayClient(myKeyId, myKeySecret);
            
            int amountInPaise = amount.multiply(new BigDecimal("100")).intValue();
            
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise); 
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

            Order order = razorpay.orders.create(orderRequest);
            System.out.println("Order successfully created! ID: " + order.get("id"));
            return order.toString(); 

        } catch (Exception e) {
            System.out.println("RAZORPAY ERROR: " + e.getMessage());
            e.printStackTrace();
            return "{\"error\": \"Failed to create order\"}";
        }
    }

    @PostMapping("/verify-payment")
    public String verifyRazorpayPayment(
            @RequestParam("razorpay_payment_id") String paymentId,
            @RequestParam("razorpay_order_id") String orderId,
            @RequestParam("razorpay_signature") String signature,
            @RequestParam("amount") BigDecimal amount,
            HttpSession session) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/";

        try {
            // Verify signature to prevent tampering
            String payload = orderId + "|" + paymentId;
            boolean isValidSignature = Utils.verifySignature(payload, signature, razorpayKeySecret);

            if (isValidSignature) {
            	boolean success = walletService.addMoneyToWallet(user.getPhoneNumber(), amount, paymentId);
                if (success) {
                    session.setAttribute("txSuccess", "₹" + amount + " successfully added via Razorpay!");
                } else {
                    session.setAttribute("txError", "Payment verified but database update failed.");
                }
            } else {
                session.setAttribute("txError", "Payment verification failed. Security signature invalid.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("txError", "An error occurred during payment processing.");
        }
        return "redirect:/dashboard";
    }
    
// --- NOTIFICATION SERVICES ---
    
    @Autowired
    private com.wallet.app.service.SmsService smsService; 

    // 1. INJECT AUTH SERVICE INSTEAD OF EMAIL SERVICE
    @Autowired
    private com.wallet.app.service.AuthService authService; 

    @PostMapping("/send-pin-otp")
    @ResponseBody
    public java.util.Map<String, Boolean> sendPinOtp(HttpSession session) {
        java.util.Map<String, Boolean> response = new java.util.HashMap<>();
        
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            response.put("success", false);
            return response;
        }

        User user = userRepo.findById(sessionUser.getId()).orElse(null);
        if (user != null) {
            // 1. Generate a new 6-digit OTP
            String otp = String.format("%06d", new java.util.Random().nextInt(999999));
            
            // 2. Save it to the database
            user.setCurrentOtp(otp);
            userRepo.save(user);
            
            boolean atLeastOneSent = false;

            // 3a. Send via Twilio SMS
            try {
                String textMessage = "PayWallet Security: Your OTP to reset your PIN is " + otp;
                smsService.sendSmsOtp(user.getPhoneNumber(), textMessage);
                atLeastOneSent = true;
            } catch (Exception e) {
                System.out.println("Failed to send PIN SMS: " + e.getMessage());
            }

            // 3b. Send via Email using your existing AuthService
            try {
                authService.sendOtpEmail(user.getEmail(), otp);
                atLeastOneSent = true;
            } catch (Exception e) {
                System.out.println("Failed to send PIN Email: " + e.getMessage());
            }
            
            if (atLeastOneSent) {
                response.put("success", true);
                return response;
            }
        }
        
        response.put("success", false);
        return response;
    }
}