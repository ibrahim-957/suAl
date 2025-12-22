package com.delivery.SuAl.helper;

import lombok.Value;

import java.math.BigDecimal;

@Value
public class ProductDepositInfo {
    Long productId;
    Integer orderQuantity;
    Integer availableContainers;
    Integer containersUsed;
    BigDecimal depositPerUnit;
    BigDecimal depositRefund;
}
