package com.example.paymentsystem.payment.messaging;

import com.example.paymentsystem.payment.config.RabbitMQConfig;
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
public class PaymentCancelRetryConsumer {

    private final PaymentClient paymentClient;
    private final PaymentService paymentService;

    /**
     * 결제 취소 실패 메시지를 1회 재시도하는 Consumer.
     *
     * 자동 재시도는 하지 않는다. 실패 원인에 관계없이 DLQ로 라우팅되며,
     * 원인 파악 후 수동으로 재발행한다.
     *
     * - RestClientException: PG사 일시 장애 등 네트워크 계열 → warn 로그 후 DLQ
     * - 그 외 Exception:     코드 버그, 잘못된 데이터 등 영구적 장애 → error 로그 후 DLQ
     */
    @RabbitListener(queues = RabbitMQConfig.PAYMENT_CANCEL_QUEUE)
    public void consume(PaymentCancelRetryMessage message) {
        String impUid = message.impUid();
        log.info("[결제 취소 재시도] impUid = {}", impUid);

        try {
            paymentClient.attemptCancelPayment(impUid);
            paymentService.markPaymentCanceled(impUid);
            log.info("[결제 취소 재시도 성공] impUid = {}", impUid);

        } catch (RestClientException e) {
            log.warn("[결제 취소 재시도 실패 - 일시적 장애] impUid = {}, 원인 = {}", impUid, e.getMessage());
            throw new AmqpRejectAndDontRequeueException(e);

        } catch (Exception e) {
            log.error("[결제 취소 재시도 실패 - 영구적 장애] impUid = {}, 원인 = {}", impUid, e.getMessage(), e);
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }
}
