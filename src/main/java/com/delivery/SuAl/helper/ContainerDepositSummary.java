package com.delivery.SuAl.helper;

import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
public class ContainerDepositSummary {
    List<ProductDepositInfo> productDepositInfoList;
    Integer totalContainersUsed;
    BigDecimal totalDepositRefunded;
}
