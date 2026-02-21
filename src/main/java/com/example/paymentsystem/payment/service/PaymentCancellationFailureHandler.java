package com.example.paymentsystem.payment.service;

import com.example.paymentsystem.payment.config.RabbitMQConfig;
import com.example.paymentsystem.payment.entity.PaymentCancelFailureHistory;
import com.example.paymentsystem.payment.messaging.PaymentCancelRetryMessage;
import com.example.paymentsystem.payment.repository.PaymentCancelFailureHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCancellationFailureHandler {

    private final PaymentCancelFailureHistoryRepository paymentCancelFailureHistoryRepository;
    private final SlackWebhookService slackService;
    private final RabbitTemplate rabbitTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentCancelFailure(String impUid, Exception exception) {
        savePaymentCancelFailureHistory(impUid, exception);
        notifyToSlack(impUid, exception);
        publishToCancelRetryQueue(impUid);
    }

    private void savePaymentCancelFailureHistory(String impUid, Exception exception) {
        try {
            PaymentCancelFailureHistory paymentCancelFailureHistory = PaymentCancelFailureHistory.builder()
                    .impUid(impUid)
                    .reason(buildFailureReason(exception))
                    .build();
            paymentCancelFailureHistoryRepository.save(paymentCancelFailureHistory);
        } catch (Exception e) {
            log.error("[결제 취소 실패 이력 저장 실패] impUid = " + impUid, e);
        }
    }

    private void publishToCancelRetryQueue(String impUid) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PAYMENT_CANCEL_EXCHANGE,
                    RabbitMQConfig.PAYMENT_CANCEL_ROUTING_KEY,
                    new PaymentCancelRetryMessage(impUid)
            );
            log.info("[결제 취소 재시도 큐 발행] impUid = {}", impUid);
        } catch (Exception e) {
            log.error("[결제 취소 재시도 큐 발행 실패] impUid = {}", impUid, e);
        }
    }

    private String buildFailureReason(Exception exception) {
        if (Objects.isNull(exception)) {
            return "UNKNOWN_EXCEPTION";
        }
        String message = exception.getMessage();
        if (!StringUtils.hasText(message)) {
            message = "NO_EXCEPTION_MESSAGE";
        }

        if (message.length() > 200) {
            message = message.substring(0, 200);
        }
        return exception.getClass().getSimpleName() + ": " + message;
    }

    private void notifyToSlack(String impUid, Exception exception) {
        try {
            slackService.sendToSlack(buildSlackMessage(impUid, exception));
        } catch (Exception e) {
            log.error("[Slack 전송 실패] impUid = " + impUid, e);
        }
    }

    private String buildSlackMessage(String impUid, Exception exception) {
        String message = "[결제 취소 실패 발생] impUid = " + impUid + ", exception = " + buildFailureReason(exception);
        return message;
    }
}
