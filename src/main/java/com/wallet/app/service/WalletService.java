package com.wallet.app.service;

import com.wallet.app.entity.User;
import com.wallet.app.entity.Wallet;
import com.wallet.app.entity.Transaction;
import com.wallet.app.repository.UserRepository;
import com.wallet.app.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WalletService {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private TransactionRepository txRepo;

    @Transactional
    public boolean sendMoney(String senderPhone, String receiverPhone, BigDecimal amount) {
        if (senderPhone.equals(receiverPhone) || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        User sender = userRepo.findByPhoneNumber(senderPhone).orElse(null);
        User receiver = userRepo.findByPhoneNumber(receiverPhone).orElse(null);

        if (sender != null && receiver != null) {
            Wallet senderWallet = sender.getWallet();
            Wallet receiverWallet = receiver.getWallet();

            if (senderWallet.getBalance().compareTo(amount) >= 0) {
                senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
                receiverWallet.setBalance(receiverWallet.getBalance().add(amount));

                userRepo.save(sender);
                userRepo.save(receiver);

                Transaction tx = new Transaction();
                tx.setSenderPhone(senderPhone);
                tx.setReceiverPhone(receiverPhone);
                tx.setAmount(amount);
                tx.setTimestamp(LocalDateTime.now());
                tx.setStatus("SUCCESS");
                txRepo.save(tx);
                return true;
            }
        }
        return false;
    }

    @Transactional
    public boolean addMoneyToWallet(String phoneNumber, BigDecimal amount, String paymentId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return false;

        User user = userRepo.findByPhoneNumber(phoneNumber).orElse(null);
        if (user == null) return false;

        Wallet wallet = user.getWallet();
        if (wallet == null) return false;

        wallet.setBalance(wallet.getBalance().add(amount));
        userRepo.save(user); 

        Transaction tx = new Transaction();
        tx.setSenderPhone(phoneNumber);   
        tx.setReceiverPhone(phoneNumber); 
        tx.setAmount(amount);
        tx.setTimestamp(LocalDateTime.now());
        tx.setStatus("BANK_DEPOSIT");
        tx.setReferenceId(paymentId); // <-- SAVES THE RAZORPAY ID
        txRepo.save(tx);

        return true;
    }

    public List<Transaction> fetchTransactionHistory(String phone) {
        return txRepo.findBySenderPhoneOrReceiverPhoneOrderByTimestampDesc(phone, phone);
    }
}