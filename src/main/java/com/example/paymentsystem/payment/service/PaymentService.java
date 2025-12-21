package com.example.paymentsystem.payment.service;

import com.example.paymentsystem.payment.dto.PaymentRequest;
import com.example.paymentsystem.payment.entity.Payment;
import com.example.paymentsystem.payment.repository.PaymentRepository;
import com.example.paymentsystem.payment.util.PaymentClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentClient paymentClient;

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }

    @Transactional
    public Payment savePayment(PaymentRequest request) {
        Payment payment = Payment.builder()
                .paymentDate(request.getPaymentDate().atStartOfDay())
                .order(request.getOrderId())
                .amount(BigDecimal.valueOf(request.getAmount()))
                .pgTid(request.getPgTid())
                .paymentMethod(request.getPaymentMethod())
                .cardName(request.getCardName())
                .cardNumber(request.getCardNumber())
                .user(request.getUserId())
                .impUid(request.getImpUid())
                .merchantUid(request.getMerchantUid())
                .partner(request.getPartnerId())
                .pgProvider(request.getPgProvider())
                .pgType(request.getPgType())
                .status(request.getStatus())
                .build();

        return paymentRepository.save(payment);
    }

    @Transactional
    public void cancelPayment(String uid) {
        Payment payment = paymentRepository.findByImpUid(uid)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found : " + uid));
        String temp = paymentClient.cancelPayment(uid);
        System.out.println(temp);
        payment.cancel();

    }
}
