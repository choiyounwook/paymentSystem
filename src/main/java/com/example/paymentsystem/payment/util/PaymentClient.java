package com.example.paymentsystem.payment.util;

import com.example.paymentsystem.payment.dto.PortOneCancelResponse;
import com.example.paymentsystem.payment.dto.TokenResponse;
import com.example.paymentsystem.payment.entity.PortOneRequestUrl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentClient {

  private final RestClient restClient;

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
      throw new RuntimeException("Failed to get access token", e);
    }
  }

  public PortOneCancelResponse cancelPayment(String impUid) {
    String accessToken = getAccessToken().getResponse().getAccess_token();

    String url = PAYMENT_URL + PortOneRequestUrl.CANCEL_PAYMENT_URL.getUrl();
    String requestBody = String.format("{\"imp_uid\": \"%s\"}", impUid);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + accessToken);

    return restClient.post().uri(url).headers(h -> h.addAll(headers)).body(requestBody).retrieve()
        .body(PortOneCancelResponse.class);
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
      throw new RuntimeException("Failed to create payment.", e);
    }
  }
}
