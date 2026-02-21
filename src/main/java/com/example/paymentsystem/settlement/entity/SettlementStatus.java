package com.example.paymentsystem.settlement.entity;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum SettlementStatus {

    REQUESTED("requested"),      // 정산 요청됨
    PROCESSING("processing"),     // 정산 처리 중
    COMPLETED("completed"),      // 정산 완료
    FAILED("failed"),         // 정산 실패
    ;

    private final String code;

    SettlementStatus(String code) {
        this.code = code;
    }

    public static SettlementStatus from(String code) {
        return Arrays.stream(values())
                .filter(status -> status.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown SettlementStatus: " + code));
    }
}
