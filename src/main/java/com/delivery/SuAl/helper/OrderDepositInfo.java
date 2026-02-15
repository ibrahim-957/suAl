package com.delivery.SuAl.helper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderDepositInfo {
    private Integer deliveryNumber;
    private BigDecimal depositCharged;
    private BigDecimal expectedDepositRefund;
    private BigDecimal netDeposit;
    private Integer containersToCollect;
}
