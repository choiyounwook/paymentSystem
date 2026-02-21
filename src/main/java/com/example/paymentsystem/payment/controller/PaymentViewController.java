package com.example.paymentsystem.payment.controller;

import com.example.paymentsystem.payment.entity.Payment;
import com.example.paymentsystem.payment.service.PaymentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
@RequestMapping("payment")
public class PaymentViewController {

  private final PaymentService paymentService;

  @Value("${payment.imp-key}")
  private String impKey;

  @Value("${payment.imp-code}")
  private String impCode;

  @GetMapping
  public String payment(Model model) {
    model.addAttribute("impKey", impKey);
    model.addAttribute("impCode", impCode);
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
