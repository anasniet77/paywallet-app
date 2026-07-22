package com.wallet.app.repository;

import com.wallet.app.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    
    // 👇 ADD THIS EXACT LINE
    Wallet findByUserId(Long userId); 
    
}