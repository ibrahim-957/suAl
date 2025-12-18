package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.OrderDetail;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderCalculationService {
    public OrderCalculationResult calculateOrderTotals(List<OrderDetail> orderDetails, Integer emptyBottlesExpected) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDepositCharged = BigDecimal.ZERO;
        int totalCount = 0;
        BigDecimal depositPerUnit = null;

        for (OrderDetail orderDetail : orderDetails) {
            subtotal = subtotal.add(orderDetail.getSubtotal());
            totalDepositCharged = totalDepositCharged.add(orderDetail.getDepositCharged());
            totalCount += orderDetail.getCount();

            if (depositPerUnit == null) {
                depositPerUnit = orderDetail.getDepositPerUnit();
            }
        }

        BigDecimal totalDepositRefunder = calculateDepositRefund(
                emptyBottlesExpected, depositPerUnit, totalCount
        );

        BigDecimal netDeposit = totalDepositCharged.subtract(totalDepositRefunder);

        return new OrderCalculationResult(
                subtotal, totalCount, totalDepositCharged, totalDepositRefunder, netDeposit, depositPerUnit
        );
    }

    private BigDecimal calculateDepositRefund(Integer emptyBottlesExpected, BigDecimal depositPerUnit, int totalCount) {
        if(emptyBottlesExpected == null || emptyBottlesExpected <= 0 || depositPerUnit == null) {
            return BigDecimal.ZERO;
        }

        int refundable = Math.min(emptyBottlesExpected, totalCount);

        return depositPerUnit.multiply(BigDecimal.valueOf(refundable))
                .setScale(2, RoundingMode.HALF_UP);

    }

    public void recalculateOrderDetail(OrderDetail orderDetail) {
        BigDecimal subtotal = orderDetail.getPricePerUnit()
                .multiply(BigDecimal.valueOf(orderDetail.getCount()))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal depositCharged = orderDetail.getDepositPerUnit()
                .multiply(BigDecimal.valueOf(orderDetail.getCount()))
                .setScale(2, RoundingMode.HALF_UP);

        orderDetail.setSubtotal(subtotal);
        orderDetail.setDepositCharged(depositCharged);
        orderDetail.setDeposit(depositCharged);
        orderDetail.setLineTotal(subtotal.add(depositCharged));
    }

}

@Value
class OrderCalculationResult {
    BigDecimal subtotal;
    int totalCount;
    BigDecimal totalDepositCharged;
    BigDecimal totalDepositRefunded;
    BigDecimal netDeposit;
    BigDecimal depositPerUnit;
}
