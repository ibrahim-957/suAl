package com.delivery.SuAl.helper;

import lombok.Value;

import java.math.BigDecimal;

@Value
public class OrderCalculationResult {
    BigDecimal subtotal;
    int totalCount;
    BigDecimal totalDepositCharged;
    BigDecimal totalDepositRefunded;
    BigDecimal netDeposit;
    BigDecimal depositPerUnit;
}
