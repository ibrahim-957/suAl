package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.entity.OrderDetail;
import com.delivery.SuAl.helper.OrderCalculationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCalculationService {
    private static final int DECIMAL_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    public OrderCalculationResult calculateOrderTotals(List<OrderDetail> orderDetails) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDepositCharged = BigDecimal.ZERO;
        BigDecimal totalDepositRefunded = BigDecimal.ZERO;
        int totalCount = 0;
        BigDecimal depositPerUnit = null;

        for (OrderDetail orderDetail : orderDetails) {
            subtotal = subtotal.add(orderDetail.getSubtotal());
            totalDepositCharged = totalDepositCharged.add(orderDetail.getDepositCharged());

            totalCount += orderDetail.getCount();

            if (depositPerUnit == null) {
                depositPerUnit = orderDetail.getDepositPerUnit();
            }

            if (orderDetail.getDepositRefunded() != null) {
                totalDepositRefunded = totalDepositRefunded.add(orderDetail.getDepositRefunded());
            }
        }

        BigDecimal netDeposit = totalDepositCharged.subtract(totalDepositRefunded);

        return new OrderCalculationResult(
                subtotal, totalCount, totalDepositCharged, totalDepositRefunded, netDeposit, depositPerUnit
        );
    }

    public void recalculateOrderDetail(OrderDetail orderDetail) {
        BigDecimal subtotal = orderDetail.getPricePerUnit()
                .multiply(BigDecimal.valueOf(orderDetail.getCount()))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal depositCharged = orderDetail.getDepositPerUnit()
                .multiply(BigDecimal.valueOf(orderDetail.getCount()))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal depositRefunded = orderDetail.getDepositRefunded() != null
                ? orderDetail.getDepositRefunded()
                : BigDecimal.ZERO;

        orderDetail.setSubtotal(subtotal);
        orderDetail.setDepositCharged(depositCharged);
        orderDetail.setLineTotal(
                subtotal.add(depositCharged)
                        .subtract(depositRefunded)
                        .setScale(2, RoundingMode.HALF_UP)
        );
    }

    public void recalculateOrderFinancials(Order order) {
        OrderCalculationResult calculation = calculateOrderTotals(order.getOrderDetails());

        if (calculation.getSubtotal() == null) {
            throw new IllegalStateException("Subtotal cannot be null");
        }

        order.setTotalItems(calculation.getTotalCount());
        order.setSubtotal(calculation.getSubtotal());
        order.setTotalDepositCharged(calculation.getTotalDepositCharged());
        order.setTotalDepositRefunded(calculation.getTotalDepositRefunded());
        order.setNetDeposit(calculation.getNetDeposit());

        BigDecimal promoDiscount = Optional.ofNullable(order.getPromoDiscount())
                .orElse(BigDecimal.ZERO);

        BigDecimal totalAmount = order.getSubtotal().add(order.getNetDeposit());

        BigDecimal amount = order.getSubtotal()
                .subtract(promoDiscount)
                .add(order.getNetDeposit());

        order.setTotalAmount(totalAmount);
        order.setAmount(amount);

        log.debug("Recalculated order financials: subtotal={}, promoDiscount={}, netDeposit={}, totalAmount={}, finalAmount={}",
                order.getSubtotal(), promoDiscount, order.getNetDeposit(), totalAmount, amount);
    }

    public void recalculateDepositsFromActualCollection(Order order) {
        log.info("Recalculating deposits for order {} based on actual collection",
                order.getOrderNumber());

        if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            log.warn("No order details to recalculate deposits for order {}", order.getOrderNumber());
            return;
        }
        BigDecimal actualDepositRefunded = BigDecimal.ZERO;

        for (OrderDetail detail : order.getOrderDetails()) {
            int actualCollected = detail.getContainersReturned();
            int delivered = detail.getCount();

            BigDecimal refundForDetail = detail.getDepositPerUnit()
                    .multiply(BigDecimal.valueOf(actualCollected))
                    .setScale(DECIMAL_SCALE, ROUNDING_MODE);

            detail.setDepositRefunded(refundForDetail);
            actualDepositRefunded = actualDepositRefunded.add(refundForDetail);

            BigDecimal lineTotal = detail.getSubtotal()
                    .add(detail.getDepositCharged())
                    .subtract(refundForDetail)
                    .setScale(DECIMAL_SCALE, ROUNDING_MODE);

            detail.setLineTotal(lineTotal);

            log.debug("Product {}: Delivered={}, Collected={}, DepositCharged={}, DepositRefunded={}, LineTotal={}",
                    detail.getProduct().getId(), delivered, actualCollected,
                    detail.getDepositCharged(), refundForDetail, lineTotal);
        }

        order.setTotalDepositRefunded(actualDepositRefunded.setScale(DECIMAL_SCALE, ROUNDING_MODE));
        order.setNetDeposit(
                order.getTotalDepositCharged()
                        .subtract(actualDepositRefunded)
                        .setScale(DECIMAL_SCALE, ROUNDING_MODE));

        BigDecimal promoDiscount = Optional.ofNullable(order.getPromoDiscount())
                .orElse(BigDecimal.ZERO);

        BigDecimal amount = order.getSubtotal()
                .subtract(promoDiscount)
                .setScale(DECIMAL_SCALE, ROUNDING_MODE);

        BigDecimal totalAmount = amount.add(order.getNetDeposit());

        order.setAmount(amount);
        order.setTotalAmount(totalAmount);

        log.info("Order {} final calculation - Subtotal: {}, PromoDiscount: {}, DepositCharged: {}, DepositRefunded: {}, NetDeposit: {}, Amount: {}, TotalAmount: {}",
                order.getOrderNumber(),
                order.getSubtotal(),
                promoDiscount,
                order.getTotalDepositCharged(),
                order.getTotalDepositRefunded(),
                order.getNetDeposit(),
                amount,
                totalAmount);
    }
}