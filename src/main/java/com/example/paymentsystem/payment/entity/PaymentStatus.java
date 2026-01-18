package com.example.paymentsystem.payment.entity;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum PaymentStatus {
    PAID("paid"),
    CANCEL_REQUESTED("cancel_requested"),
    CANCELED("cancel"),
    CANCEL_FAILED("cancel_failed"),
    ;

    private final String code;

    PaymentStatus(String code) {
        this.code = code;
    }

    public static PaymentStatus from(String code) {
        return Arrays.stream(values())
                .filter(status -> status.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown PaymentStatus: " + code));
    }

}
