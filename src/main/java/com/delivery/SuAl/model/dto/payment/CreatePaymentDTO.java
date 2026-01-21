package com.delivery.SuAl.model.dto.payment;

import com.delivery.SuAl.model.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreatePaymentDTO {
    private Long orderId;
    private BigDecimal amount;
    private String currencyCode;
    private TransactionType type;
    private String description;
    private String language;
    private String cardToken;
    private boolean saveCard;
}