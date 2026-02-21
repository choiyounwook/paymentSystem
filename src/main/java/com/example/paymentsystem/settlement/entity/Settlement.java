package com.example.paymentsystem.settlement.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@Table(name = "settlement")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "partner_id", nullable = false)
    Long partnerId;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    SettlementStatus status;

    @Column(name = "payment_date", nullable = false)
    LocalDateTime paymentDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    LocalDateTime updatedAt;

    @Builder
    public Settlement(Long partnerId, BigDecimal amount, SettlementStatus status, LocalDateTime paymentDate) {
        this.partnerId = partnerId;
        this.amount = amount;
        this.status = status;
        this.paymentDate = paymentDate;
    }


    public static Settlement createCompletedSettlement(Long partnerId, BigDecimal amount, LocalDateTime paymentDate) {
        Settlement settlement = Settlement.builder()
                .partnerId(partnerId)
                .amount(amount)
                .paymentDate(paymentDate)
                .status(SettlementStatus.COMPLETED)
                .build();
        return settlement;
    }
}
