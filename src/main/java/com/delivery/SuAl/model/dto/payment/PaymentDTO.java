package com.delivery.SuAl.model.dto.payment;

import com.delivery.SuAl.model.enums.PaymentMethod;
import com.delivery.SuAl.model.enums.PaymentStatus;
import com.delivery.SuAl.model.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentDTO {
    private Long id;
    private Long orderId;
    private String referenceId;
    private BigDecimal amount;
    private BigDecimal fee;
    private String currencyCode;
    private TransactionType transactionType;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String gatewayPaymentUrl;
    private String maskedPan;
    private String cardSystem;
    private String cardIssuer;
    private String rrn;
    private String approvalCode;
    private String threeDsStatus;
    private String failureReason;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}