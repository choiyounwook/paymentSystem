package com.example.paymentsystem.payment.controller;

import com.example.paymentsystem.payment.dto.PaymentRequest;
import com.example.paymentsystem.payment.entity.Payment;
import com.example.paymentsystem.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;


    /**
     * 결제 결과 DB에 삽입
     */
    @PostMapping("portone")
    public ResponseEntity<String> savePortone(@RequestBody PaymentRequest request) {
        try {
            System.out.println("start db insert");
            paymentService.savePayment(request);
            return ResponseEntity.ok("Payment insert processed successfully.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save portone.");
        }
    }

    /**
     * 모든 결제 내역 조회
     */
    @GetMapping("list")
    public ResponseEntity<List<Payment>> getPayments() {
        List<Payment> paymentList = paymentService.getAllPayments();
        return ResponseEntity.ok(paymentList);
    }

    @GetMapping("cancel/{uid}")
    public ResponseEntity<String> cancelPayment(@PathVariable("uid") String uid) {
        paymentService.cancelPayment(uid);
        return ResponseEntity.ok("Payment cancel processed successfully.");
    }

}
