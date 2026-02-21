package com.example.paymentsystem;

import com.example.paymentsystem.common.exception.ServiceException;
import com.example.paymentsystem.common.response.ApiResponse;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ServiceException.class)
  public ResponseEntity<?> handleException(ServiceException exception) {
    return ApiResponse.ResponseException(exception.getCode(), exception.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> methodArgumentNotValidException(
      MethodArgumentNotValidException exception) {
    String errorMessage = exception.getBindingResult()
        .getAllErrors()
        .stream()
        .map(error -> error.getDefaultMessage())
        .collect(Collectors.joining(", "));
    return ApiResponse.ValidException("VALIDATE_ERROR", errorMessage);
  }

  @ExceptionHandler(BindException.class)
  public ResponseEntity<?> bindException(BindException exception) {
    AtomicReference<String> errors = new AtomicReference<>("");
    exception.getBindingResult().getAllErrors().forEach(c -> errors.set(c.getDefaultMessage()));
    return ApiResponse.ValidException("VALIDATE_ERROR", String.valueOf(errors));
  }

  @ExceptionHandler(value = Exception.class)
  public ResponseEntity<?> handleException(Exception exception) {
    return ApiResponse.ServerException("SERVER_ERROR", exception.getMessage());
  }
}
