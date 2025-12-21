package com.example.paymentsystem.payment.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TokenResponse {

  int code;
  String message;
  TokenInfo response;
}
