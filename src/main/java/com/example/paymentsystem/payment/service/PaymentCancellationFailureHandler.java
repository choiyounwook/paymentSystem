package com.example.paymentsystem.payment.service;

import com.example.paymentsystem.payment.entity.PaymentCancelFailureHistory;
import com.example.paymentsystem.payment.repository.PaymentCancelFailureHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCancellationFailureHandler {

  private final PaymentCancelFailureHistoryRepository paymentCancelFailureHistoryRepository;
  private final SlackWebhookService slackService;
  private final PaymentService paymentService;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handlePaymentCancelFailure(String impUid, String errorMessage) {
    savePaymentCancelFailureHistory(impUid, errorMessage);
    paymentService.markPaymentCancelFailed(impUid);
    notifyToSlack(impUid, errorMessage);
  }

  private void savePaymentCancelFailureHistory(String impUid, String errorMessage) {
    try {
      PaymentCancelFailureHistory history = PaymentCancelFailureHistory.builder()
          .impUid(impUid)
          .reason(buildFailureReason(errorMessage))
          .build();
      paymentCancelFailureHistoryRepository.save(history);
    } catch (Exception e) {
      log.error("[결제 취소 실패 이력 저장 실패] impUid = {}", impUid, e);
    }
  }

  private void notifyToSlack(String impUid, String errorMessage) {
    try {
      slackService.sendToSlack("[결제 취소 최종 실패] impUid = " + impUid + ", 오류 = " + buildFailureReason(errorMessage));
    } catch (Exception e) {
      log.error("[Slack 전송 실패] impUid = {}", impUid, e);
    }
  }

  private String buildFailureReason(String errorMessage) {
    if (!StringUtils.hasText(errorMessage)) {
      return "UNKNOWN_ERROR";
    }
    return errorMessage.length() > 200 ? errorMessage.substring(0, 200) : errorMessage;
  }
}
