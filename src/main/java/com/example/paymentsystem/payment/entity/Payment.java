package com.example.paymentsystem.payment.entity;

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
@Table(name = "payments")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "partner_id")
    Long partner;

    @Column(name = "user_id", nullable = false)
    Long user;

    @Column(name = "order_id", nullable = false)
    Long order;

    @Column(name = "payment_amount", nullable = false, precision = 15, scale = 2)
    BigDecimal amount;

    @Column(name = "payment_date", nullable = false)
    LocalDateTime paymentDate;

    @Column(name = "imp_uid", nullable = false)
    String impUid;

    @Column(name = "payment_method", nullable = false)
    String paymentMethod;

    @Column(name = "merchant_uid", nullable = false)
    String merchantUid;

    @Column(name = "pg_provider", nullable = false)
    String pgProvider;

    @Column(name = "pg_type", nullable = false)
    String pgType;

    @Column(name = "pg_tid", nullable = false)
    String pgTid;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    PaymentStatus status;

    @Column(name = "card_name")
    String cardName;

    @Column(name = "card_number")
    String cardNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    LocalDateTime updatedAt;

    @Builder
    public Payment(Long partner, Long user, Long order, BigDecimal amount, LocalDateTime paymentDate,
                   String impUid, String paymentMethod, String merchantUid, String pgProvider, String pgType,
                   String pgTid, PaymentStatus status, String cardName, String cardNumber) {
        this.partner = partner;
        this.user = user;
        this.order = order;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.impUid = impUid;
        this.paymentMethod = paymentMethod;
        this.merchantUid = merchantUid;
        this.pgProvider = pgProvider;
        this.pgType = pgType;
        this.pgTid = pgTid;
        this.status = status;
        this.cardName = cardName;
        this.cardNumber = cardNumber;
    }

    public void markCancleRequested() {
        this.status = PaymentStatus.CANCEL_REQUESTED;
    }

    public void markCancel() {
        this.status = PaymentStatus.CANCELED;
    }

    public void markCancelFailed() {
        this.status = PaymentStatus.CANCEL_FAILED;
    }
}
