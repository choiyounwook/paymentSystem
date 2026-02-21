package com.example.paymentsystem.payment.repository;

import com.example.paymentsystem.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByImpUid(String impUid);

    List<Payment> findByPaymentDateBetween(LocalDateTime startDate, LocalDateTime endDate);
}
