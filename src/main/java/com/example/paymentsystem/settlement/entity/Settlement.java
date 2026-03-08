package com.example.paymentsystem.settlement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Entity
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@Table(name = "settlements")
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

  @Column(name = "settlement_date", nullable = false)
  LocalDateTime settlementDate;

  @Column(name = "created_at", nullable = false, updatable = false)
  @CreationTimestamp
  LocalDateTime createdAt;

  @Column(name = "updated_at")
  @UpdateTimestamp
  LocalDateTime updatedAt;

  @Builder
  public Settlement(Long partnerId, BigDecimal amount, SettlementStatus status,
      LocalDateTime settlementDate) {
    this.partnerId = partnerId;
    this.amount = amount;
    this.status = status;
    this.settlementDate = settlementDate;
  }

  public static Settlement createCompletedSettlement(Long partnerId, BigDecimal amount,
      LocalDate settlementDate) {
    return Settlement.builder()
        .partnerId(partnerId)
        .amount(amount)
        .settlementDate(settlementDate.atStartOfDay())
        .status(SettlementStatus.COMPLETED)
        .build();
  }
}
