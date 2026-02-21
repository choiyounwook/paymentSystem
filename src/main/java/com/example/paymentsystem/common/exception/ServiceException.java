package com.example.paymentsystem.common.exception;

import com.example.paymentsystem.common.exception.code.PaymentExceptionCode;
import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {

  private String code;
  private String message;

  public ServiceException(PaymentExceptionCode exceptionCode) {
    super(exceptionCode.getMessage());
    this.code = exceptionCode.getCode();
    this.message = exceptionCode.getMessage();
  }

}
