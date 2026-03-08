package com.example.paymentsystem.payment.messaging;

import com.example.paymentsystem.common.config.RabbitMQConfig;
import com.example.paymentsystem.payment.dto.PaymentCancelMessage;
import com.example.paymentsystem.payment.service.PaymentCancellationFailureHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCancelDlqConsumer {

  private final PaymentCancellationFailureHandler paymentCancellationFailureHandler;

  /**
   * 결제 취소 재시도가 모두 소진된 후 DLQ로 라우팅된 메시지를 처리하는 Consumer. 실패 이력 저장, 상태 업데이트, 긴급 Slack 알림을 수행한다.
   */
  @RabbitListener(queues = RabbitMQConfig.PAYMENT_CANCEL_DLQ)
  public void consume(
      PaymentCancelMessage message,
      @Header(value = RepublishMessageRecoverer.X_EXCEPTION_MESSAGE, required = false) String errorMessage
  ) {
    String impUid = message.impUid();
    log.error("[결제 취소 DLQ] impUid = {}, 오류 = {}", impUid, errorMessage);
    paymentCancellationFailureHandler.handlePaymentCancelFailure(impUid, errorMessage);
  }
}
