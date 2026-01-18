package com.example.paymentsystem.payment.repository;

import com.example.paymentsystem.payment.entity.PaymentCancelFailureHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentCancelFailureHistoryRepository extends JpaRepository<PaymentCancelFailureHistory, Long> {
}
