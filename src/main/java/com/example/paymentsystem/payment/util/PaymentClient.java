package com.example.paymentsystem.payment.util;

import com.example.paymentsystem.payment.dto.TokenResponse;
import com.example.paymentsystem.payment.entity.PortOneRequestUrl;
import com.example.paymentsystem.payment.service.PaymentCancellationFailureHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentClient {

  private final RestClient restClient;
  private final PaymentCancellationFailureHandler paymentCancellationFailureHandler;
  private final boolean failTest = true;

  @Value("${payment.imp-key}")
  private String impKey;

  @Value("${payment.imp-secret}")
  private String impSecret;

  private static final String PAYMENT_URL = "https://api.iamport.kr";

  public TokenResponse getAccessToken() {
    String url = PAYMENT_URL + PortOneRequestUrl.ACCESS_TOKEN_URL.getUrl();
    try {
      String requestBody = String.format("{\"imp_key\": \"%s\", \"imp_secret\": \"%s\"}", impKey,
          impSecret);
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      return restClient.post().uri(url).headers(h -> h.addAll(headers)).body(requestBody).retrieve()
          .body(TokenResponse.class);

    } catch (RuntimeException e) {
      throw new RuntimeException("Failed to get access token");
    }
  }


  @Retryable(
      value = RestClientException.class, // 재시도할 예외 트리거
      maxAttempts = 2, // 최초 호출 1회 + 재시도 1회
      backoff = @Backoff(delay = 2000), // 2초 대기 후 재시도
      recover = "handlePaymentCancellationFailure" // 모든 재시도 소진 후 후처리 메소드
  )
  public String cancelPayment(String impUid) {
    String accessToken = getAccessToken().getResponse().getAccess_token();

    String url = PAYMENT_URL + PortOneRequestUrl.CANCEL_PAYMENT_URL.getUrl();
    String requestBody = String.format("{\"imp_uid\": \"%s\"}", impUid);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + accessToken);

    if (failTest) {
      // 결제 실패 테스트
      throw new RestClientException("Forced RestClientException for testing");
    } else {
      return restClient.post().uri(url).headers(h -> h.addAll(headers)).body(requestBody).retrieve()
          .body(String.class);
    }
  }

  @Recover
  public String handlePaymentCancellationFailure(RestClientException e, String impUid) {
    log.error("RestClientException 예외로 인해 실패: " + e.getClass().getName() + " impUid : " + impUid);
    paymentCancellationFailureHandler.handlePaymentCancelFailure(impUid, e);
    throw e;
  }

  public String attemptCancelPayment(String impUid) {
    String accessToken = getAccessToken().getResponse().getAccess_token();

    String url = PAYMENT_URL + PortOneRequestUrl.CANCEL_PAYMENT_URL.getUrl();
    String requestBody = String.format("{\"imp_uid\": \"%s\"}", impUid);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + accessToken);

    return restClient.post().uri(url).headers(h -> h.addAll(headers)).body(requestBody).retrieve()
        .body(String.class);
  }

  public String createPayment(String paymentRequest, String token) {
    String url = PAYMENT_URL + PortOneRequestUrl.CREATE_PAYMENT_URL.getUrl();
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.setBasicAuth(token);

      return restClient.post().uri(url).headers(h -> h.addAll(headers)).body(paymentRequest)
          .retrieve()
          .body(String.class);
    } catch (RestClientException e) {
      throw new RuntimeException("Failed to create payment.");
    }
  }
}
