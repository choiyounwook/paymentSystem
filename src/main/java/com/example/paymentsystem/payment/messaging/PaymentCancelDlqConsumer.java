package com.example.paymentsystem.payment.messaging;

import com.example.paymentsystem.payment.config.RabbitMQConfig;
import com.example.paymentsystem.payment.service.SlackWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCancelDlqConsumer {

    private final SlackWebhookService slackWebhookService;

    /**
     * 결제 취소 재시도가 모두 소진된 후 DLQ로 라우팅된 메시지를 처리하는 Consumer.
     * 수동 개입이 필요하므로 긴급 Slack 알림을 발송한다.
     */
    @RabbitListener(queues = RabbitMQConfig.PAYMENT_CANCEL_DLQ)
    public void consume(PaymentCancelRetryMessage message) {
        String impUid = message.impUid();
        log.error("[결제 취소 최종 실패 - 수동 처리 필요] impUid = {}", impUid);
        slackWebhookService.sendToSlack("[결제 취소 최종 실패 - 수동 처리 필요] impUid = " + impUid);
    }
}
