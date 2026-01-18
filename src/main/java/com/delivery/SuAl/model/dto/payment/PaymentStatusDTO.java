package com.delivery.SuAl.model.dto.payment;

import com.delivery.SuAl.model.enums.PaymentStatus;
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
public class PaymentStatusDTO {
    private String referenceId;
    private PaymentStatus status;
    private BigDecimal amount;
    private String maskedPan;
    private String cardSystem;
    private LocalDateTime paidAt;
    private String failureReason;
}