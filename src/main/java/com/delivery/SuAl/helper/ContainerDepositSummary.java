package com.delivery.SuAl.helper;

import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Value
public class ContainerDepositSummary {
    List<ProductDepositInfo> productDepositInfos;
    Integer totalContainersUsed;
    BigDecimal totalDepositRefunded;

    public Map<Long, Integer> getContainersReturnedByProduct() {
        return productDepositInfos.stream()
                .collect(Collectors.toMap(
                        ProductDepositInfo::getProductId,
                        ProductDepositInfo::getContainersUsed
                ));
    }
}
