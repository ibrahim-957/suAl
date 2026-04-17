package com.delivery.SuAl.helper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageDepositSummary {
    private BigDecimal totalDepositCharged;
    private BigDecimal expectedDepositRefund;
    private BigDecimal netDeposit;
    private Integer oldContainersToCollect;
    private Integer totalContainersInPackage;
}
