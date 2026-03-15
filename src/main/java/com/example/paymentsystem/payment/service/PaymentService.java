package com.example.paymentsystem.payment.service;

import com.example.paymentsystem.common.config.RabbitMQConfig;
import com.example.paymentsystem.common.exception.ServiceException;
import com.example.paymentsystem.common.exception.code.PaymentExceptionCode;
import com.example.paymentsystem.payment.dto.PaymentCancelMessage;
import com.example.paymentsystem.payment.dto.PaymentRequest;
import com.example.paymentsystem.payment.entity.Payment;
import com.example.paymentsystem.payment.entity.PaymentStatus;
import com.example.paymentsystem.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
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

    Payment payment = getPayment(request, portOneStatus);

    return paymentRepository.save(payment);
  }

  @Transactional
  public void markPaymentCanceled(String impUid) {
    getPaymentByUId(impUid).markCancel();
  }

  @Transactional
  public void markPaymentCancelFailed(String impUid) {
    getPaymentByUId(impUid).markCancelFailed();
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

  @Transactional
  public void createDummyPayments(int count) {
    Random random = new Random();
    List<Payment> payments = IntStream.range(0, count).mapToObj(i -> {
      String uid = "dummy_" + System.currentTimeMillis() + "_" + i;
      return Payment.builder()
          .partner((long) (random.nextInt(30) + 1))
          .user((long) (random.nextInt(100) + 1))
          .order((long) (random.nextInt(1000) + 1))
          .amount(BigDecimal.valueOf((random.nextInt(100) + 1) * 1000))
          .paymentDate(LocalDateTime.now().minusDays(1))
          .impUid(uid)
          .paymentMethod("card")
          .merchantUid("IMP_" + uid)
          .pgProvider("kakaopay")
          .pgType("payment")
          .pgTid("test_tid_" + uid)
          .status(PaymentStatus.PAID)
          .cardName("카카오뱅크")
          .cardNumber("1234-****-****-5678")
          .build();
    }).toList();
    paymentRepository.saveAll(payments);
  }

  private Payment getPaymentByUId(String uid) {
    return paymentRepository.findByImpUid(uid)
        .orElseThrow(() -> new ServiceException(PaymentExceptionCode.NOT_FOUND_PAYMENT));
  }

  private Payment getPayment(PaymentRequest request, PaymentStatus status) {
    return Payment.builder()
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
        .status(status)
        .build();
  }
}
