package com.example.paymentsystem.payment.repository;

import com.example.paymentsystem.payment.entity.Payment;
import com.example.paymentsystem.payment.entity.PaymentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

  List<Payment> findAllByOrderByIdDesc();

  Optional<Payment> findByImpUid(String impUid);

  List<Payment> findByPaymentDateBetween(LocalDateTime startDate, LocalDateTime endDate);

  List<Payment> findByPaymentDateBetweenAndStatus(LocalDateTime startDate, LocalDateTime endDate,
      PaymentStatus paymentStatus);
}
