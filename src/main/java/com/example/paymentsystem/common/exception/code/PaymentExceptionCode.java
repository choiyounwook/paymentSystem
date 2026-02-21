package com.example.paymentsystem.common.exception.code;

import lombok.Getter;

@Getter
public enum PaymentExceptionCode {
  NOT_FOUND_PAYMENT("NOT_FOUND_PAYMENT", "결제 내역을 찾을 수 없습니다."),
  INVALID_PAYMENT_STATUS("INVALID_PAYMENT_STATUS", "PAID 상태의 결제만 저장할 수 있습니다."),
  CANNOT_CANCEL_PAYMENT("CANNOT_CANCEL_PAYMENT", "취소 가능한 상태의 결제가 아닙니다."),
  ;

  private final String code;
  private final String message;

  PaymentExceptionCode(String code, String message) {
    this.code = code;
    this.message = message;
  }
}
