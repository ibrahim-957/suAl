package com.delivery.SuAl.entity;

import com.delivery.SuAl.model.enums.PaymentMethod;
import com.delivery.SuAl.model.enums.PaymentProvider;
import com.delivery.SuAl.model.enums.PaymentStatus;
import com.delivery.SuAl.model.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id",  nullable = false)
    private Order order;

    @Column(name = "reference_id", unique = true, nullable = false)
    private String referenceId;

    @Column(nullable = false, precision = 10, scale = 2, name = "amount_in_coins")
    private Long amountInCoins;

    @Column(precision = 10, scale = 2, name = "fee_in_coins")
    private Long feeInCoins;

    @Column(name = "currency_code", nullable = false)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "payment_method")
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_provider", nullable = false)
    private PaymentProvider paymentProvider;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus =  PaymentStatus.CREATED;

    @Column(name = "gateway_payment_url", columnDefinition = "TEXT")
    private String gatewayPaymentUrl;

    @Column(name = "gateway_transaction_id")
    private String gatewayTransactionId;

    private String rrn;

    @Column(name = "approval_code")
    private String approvalCode;

    @Column(name = "masked_pan")
    private String maskedPan;

    @Column(name = "card_token")
    private String cardToken;

    @Column(name = "card_issuer")
    private String cardIssuer;

    @Column(name = "three_ds_status")
    private String threeDsStatus;

    @Column(name = "raw_create_response", columnDefinition = "TEXT")
    private String rawCreateResponse;

    @Column(name = "raw_status_response", columnDefinition = "TEXT")
    private String rawStatusResponse;

    @Column(name = "raw_callback_response", columnDefinition = "TEXT")
    private String rawCallbackResponse;

    @Column(name = "gateway_status_code")
    private String gatewayStatusCode;

    @Column(name = "gateway_response_code")
    private String gatewayResponseCode;

    @Column(name = "gateway_message")
    private String gatewayMessage;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "refund_amount_in_coins", precision = 10, scale = 2)
    private Long refundAmountInCoins;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payment_datetime")
    private LocalDateTime paymentDatetime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public BigDecimal getAmountAsDecimal(){
        return amountInCoins != null
                ? new BigDecimal(amountInCoins).divide(new BigDecimal("100"), 2,  RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }

    public BigDecimal getFeeAsDecimal(){
        return feeInCoins != null
                ? new BigDecimal(feeInCoins).divide(new BigDecimal("100"), 2,  RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }

    public BigDecimal getRefundAmountAsDecimal() {
        return refundAmountInCoins != null
                ? new BigDecimal(refundAmountInCoins).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }
}