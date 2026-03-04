package com.example.paymentsystem.payment.service;

import com.example.paymentsystem.common.exception.ServiceException;
import com.example.paymentsystem.common.exception.code.PaymentExceptionCode;
import com.example.paymentsystem.payment.config.RabbitMQConfig;
import com.example.paymentsystem.payment.dto.PaymentCancelMessage;
import com.example.paymentsystem.payment.dto.PaymentRequest;
import com.example.paymentsystem.payment.entity.Payment;
import com.example.paymentsystem.payment.entity.PaymentStatus;
import com.example.paymentsystem.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final RabbitTemplate rabbitTemplate;

  public String getPaymentStatus(String impUid) {
    return getPaymentByUId(impUid).getStatus().name();
  }

  public List<Payment> getAllPayments() {
    return paymentRepository.findAllByOrderByIdDesc();
  }

  @Transactional
  public Payment savePayment(PaymentRequest request) {
    PaymentStatus portOneStatus = PaymentStatus.from(request.getStatus());

    if (portOneStatus != PaymentStatus.PAID) {
      throw new ServiceException(PaymentExceptionCode.INVALID_PAYMENT_STATUS);
    }

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
        .status(portOneStatus)
        .build();

    return paymentRepository.save(payment);
  }

  @Transactional
  public void markPaymentCanceled(String impUid) {
    paymentRepository.findByImpUid(impUid)
        .ifPresent(Payment::markCancel);
  }

  @Transactional
  public void markPaymentCancelFailed(String impUid) {
    paymentRepository.findByImpUid(impUid)
        .ifPresent(Payment::markCancelFailed);
  }

  @Transactional
  public void cancelPayment(String uid) {
    Payment payment = getPaymentByUId(uid);

    if (!payment.isCancelable()) {
      throw new ServiceException(PaymentExceptionCode.CANNOT_CANCEL_PAYMENT);
    }

    payment.markCancelRequested();
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.PAYMENT_CANCEL_EXCHANGE,
        RabbitMQConfig.PAYMENT_CANCEL_ROUTING_KEY,
        new PaymentCancelMessage(payment.getImpUid())
    );
    log.info("[결제 취소 메시지 전송] impUid = {}", payment.getImpUid());
  }

  private Payment getPaymentByUId(String uid) {
    return paymentRepository.findByImpUid(uid)
        .orElseThrow(() -> new ServiceException(PaymentExceptionCode.NOT_FOUND_PAYMENT));
  }
}
