package com.example.paymentsystem.common.response;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

@Getter
public class ApiResponse<T> {

  private final Boolean success;
  private final Error error;
  private final T message;

  public ApiResponse(Boolean success, String error, String errorMessage, T message) {
    this.success = success;
    this.error = Error.builder()
        .errorCode(error)
        .errorMessage(errorMessage)
        .build();
    this.message = message;
  }

  public static <T> ApiResponse<T> success(T result) {
    return new ApiResponse<>(true, null, null, result);
  }

  public static <T> ApiResponse<T> fail(T result) {
    return new ApiResponse<>(false, null, null, result);
  }

  public static <T> ResponseEntity<ApiResponse<T>> ResponseException(String code,
      String errorMessage) {
    return ResponseEntity.ok(new ApiResponse<>(false, code, errorMessage, null));
  }

  public static <T> ResponseEntity<ApiResponse<T>> ValidException(String code,
      String errorMessage) {
    return ResponseEntity.status(400).body(new ApiResponse<>(false, code, errorMessage, null));
  }

  public static <T> ResponseEntity<ApiResponse<T>> ServerException(String code,
      String errorMessage) {
    return ResponseEntity.status(500).body(new ApiResponse<>(false, code, errorMessage, null));
  }

  @Getter
  public static class Error {

    private final String errorCode;
    private final String errorMessage;

    @Builder
    public Error(String errorCode, String errorMessage) {
      this.errorCode = errorCode;
      this.errorMessage = errorMessage;
    }
  }
}
