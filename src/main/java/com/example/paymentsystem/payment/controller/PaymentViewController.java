package com.example.paymentsystem.payment.controller;

import com.example.paymentsystem.payment.entity.Payment;
import com.example.paymentsystem.payment.service.PaymentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
@RequestMapping("payment")
public class PaymentViewController {

  private final PaymentService paymentService;

  @GetMapping
  public String payment() {
    return "payment";
  }

  @GetMapping("mypage")
  public ModelAndView MyPage() {
    List<Payment> paymentList = paymentService.getAllPayments();

    ModelAndView modelAndView = new ModelAndView("mypage");
    modelAndView.addObject("paymentList", paymentList);

    return modelAndView;
  }
}
