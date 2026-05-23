package com.reconcileguard.repository;

import com.reconcileguard.domain.PaymentChannel;
import com.reconcileguard.domain.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, String> {
    List<PaymentTransaction> findByChannel(PaymentChannel channel);

    @Query("select t.utr from PaymentTransaction t group by t.utr having count(t) > 1")
    List<String> findDuplicateUtrs();
}
