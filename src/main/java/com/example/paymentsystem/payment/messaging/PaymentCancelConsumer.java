package com.example.paymentsystem.payment.messaging;

import com.example.paymentsystem.common.config.RabbitMQConfig;
import com.example.paymentsystem.payment.dto.PaymentCancelMessage;
import com.example.paymentsystem.payment.service.PaymentService;
import com.example.paymentsystem.payment.util.PaymentClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCancelConsumer {

  private final PaymentClient paymentClient;
  private final PaymentService paymentService;

  /**
   * 결제 취소 메시지 Consumer.
   */
  @RabbitListener(queues = RabbitMQConfig.PAYMENT_CANCEL_QUEUE)
  public void consume(PaymentCancelMessage message) {
    String impUid = message.impUid();
    log.info("[결제 취소 요청 수행] impUid = {}", impUid);

    try {
      paymentClient.cancelPayment(impUid);
      paymentService.markPaymentCanceled(impUid);
      log.info("[결제 취소 성공] impUid = {}", impUid);

    } catch (RestClientException e) {
      // 일시적 장애 (네트워크, PG사 일시 다운) → Spring AMQP retry 위임
      log.warn("[결제 취소 실패 - 일시적 장애] impUid = {}", impUid, e);
      throw e;

    } catch (Exception e) {
      // 영구적 장애 (코드 버그, 잘못된 데이터) → retry 불필요, 즉시 DLQ
      log.error("[결제 취소 실패 - 영구적 장애] impUid = {}", impUid, e);
      throw new AmqpRejectAndDontRequeueException(e);
    }
  }
}
