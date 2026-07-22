package com.wallet.app.repository;

import com.wallet.app.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findBySenderPhoneOrReceiverPhoneOrderByTimestampDesc(String senderPhone, String receiverPhone);
}