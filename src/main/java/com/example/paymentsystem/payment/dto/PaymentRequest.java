package com.example.paymentsystem.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentRequest {

  Long partnerId;
  Long userId;
  Long orderId;
  LocalDate paymentDate;
  @JsonProperty("imp_uid")
  String impUid;
  @JsonProperty("pay_method")
  String paymentMethod;
  @JsonProperty("merchant_uid")
  String merchantUid;
  @JsonProperty("paid_amount")
  int amount;
  @JsonProperty("pg_provider")
  String pgProvider;
  @JsonProperty("pg_type")
  String pgType;
  @JsonProperty("pg_tid")
  String pgTid;
  String status;
  @JsonProperty("card_name")
  String cardName;
  @JsonProperty("card_number")
  String cardNumber;
}
