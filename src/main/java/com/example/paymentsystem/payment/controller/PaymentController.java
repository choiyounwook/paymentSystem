package com.example.paymentsystem.payment.controller;

import com.example.paymentsystem.common.response.ApiResponse;
import com.example.paymentsystem.payment.dto.PaymentRequest;
import com.example.paymentsystem.payment.entity.Payment;
import com.example.paymentsystem.payment.service.PaymentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class PaymentController {

  private final PaymentService paymentService;


  /**
   * 결제 결과 DB에 삽입
   */
  @PostMapping("portone")
  public ApiResponse<String> savePortone(@RequestBody PaymentRequest request) {
    System.out.println("start db insert");
    paymentService.savePayment(request);
    return ApiResponse.success("Payment insert processed successfully.");
  }

  /**
   * 모든 결제 내역 조회
   */
  @GetMapping("list")
  public ApiResponse<List<Payment>> getPayments() {
    return ApiResponse.success(paymentService.getAllPayments());
  }

  /**
   * 결제 취소
   */
  @GetMapping("cancel/{uid}")
  public ApiResponse<String> cancelPayment(@PathVariable("uid") String uid) {
    paymentService.cancelPayment(uid);
    return ApiResponse.success("Payment cancel processed successfully.");
  }

}
