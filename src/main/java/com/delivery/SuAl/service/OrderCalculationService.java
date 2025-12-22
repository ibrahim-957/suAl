package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.entity.OrderDetail;
import com.delivery.SuAl.helper.OrderCalculationResult;
import com.delivery.SuAl.helper.PromoDiscountResult;
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

            if (orderDetail.getDepositRefunded() != null)
                totalDepositRefunded = totalDepositRefunded.add(orderDetail.getDepositRefunded());
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

        orderDetail.setSubtotal(subtotal);
        orderDetail.setDepositCharged(depositCharged);
        orderDetail.setLineTotal(subtotal.add(depositCharged).subtract(
                orderDetail.getDepositRefunded() != null ? orderDetail.getDepositRefunded() : BigDecimal.ZERO
        ));
    }

    public void applyCalculationAndPromoToOrder(
            Order order,
            OrderCalculationResult calculation,
            PromoDiscountResult promoResult
    ) {
        order.setTotalItems(calculation.getTotalCount());
        order.setSubtotal(calculation.getSubtotal());
        order.setTotalDepositCharged(calculation.getTotalDepositCharged());
        order.setTotalDepositRefunded(calculation.getTotalDepositRefunded());
        order.setNetDeposit(calculation.getNetDeposit());

        if (promoResult.hasPromo()) {
            order.setPromo(promoResult.getPromo());
            order.setPromoDiscount(promoResult.getDiscount());
        }

        BigDecimal promoDiscount = Optional.ofNullable(order.getPromoDiscount())
                .orElse(BigDecimal.ZERO);

        BigDecimal amount = order.getSubtotal().subtract(promoDiscount);
        BigDecimal totalAmount = amount.add(order.getNetDeposit());

        order.setAmount(amount);
        order.setTotalAmount(totalAmount);

        log.debug("Applied calculation to order: subtotal={}, deposit={}, promo={}, total={}",
                order.getSubtotal(), order.getNetDeposit(), promoDiscount, totalAmount);
    }

    public void recalculateOrderFinancials(Order order) {
        OrderCalculationResult calculation = calculateOrderTotals(order.getOrderDetails());

        order.setTotalItems(calculation.getTotalCount());
        order.setSubtotal(calculation.getSubtotal());
        order.setTotalDepositCharged(calculation.getTotalDepositCharged());
        order.setTotalDepositRefunded(calculation.getTotalDepositRefunded());
        order.setNetDeposit(calculation.getNetDeposit());

        BigDecimal promoDiscount = Optional.ofNullable(order.getPromoDiscount())
                .orElse(BigDecimal.ZERO);

        BigDecimal amount = order.getSubtotal().subtract(promoDiscount);
        BigDecimal totalAmount = amount.add(order.getNetDeposit());

        order.setAmount(amount);
        order.setTotalAmount(totalAmount);

        log.debug("Recalculated order financials: subtotal={}, net deposit={}, total={}",
                order.getSubtotal(), order.getNetDeposit(), totalAmount);
    }

    public void recalculateDepositsFromActualCollection(Order order) {
        log.info("Recalculating deposits for order {} based on actual collection",
                order.getOrderNumber());

        BigDecimal actualDepositRefunded = BigDecimal.ZERO;

        for (OrderDetail detail : order.getOrderDetails()) {
            int actualCollected = detail.getContainersReturned();
            int delivered = detail.getCount();

            BigDecimal refundForDetail = detail.getDepositPerUnit()
                    .multiply(BigDecimal.valueOf(actualCollected))
                    .setScale(2, RoundingMode.HALF_UP);

            detail.setDepositRefunded(refundForDetail);
            actualDepositRefunded = actualDepositRefunded.add(refundForDetail);

            detail.setLineTotal(
                    detail.getSubtotal()
                            .add(actualDepositRefunded)
                            .subtract(detail.getDepositRefunded())
            );

            log.debug("Product {}: Delivered={}, Collected={}, DepositCharged={}, DepositRefunded={}, LineTotal={}",
                    detail.getProduct().getId(),
                    delivered,
                    actualCollected,
                    detail.getDepositCharged(),
                    refundForDetail,
                    detail.getLineTotal());
        }

        order.setTotalDepositRefunded(actualDepositRefunded);
        order.setNetDeposit(order.getTotalDepositCharged().subtract(actualDepositRefunded));

        BigDecimal promoDiscount = Optional.ofNullable(order.getPromoDiscount())
                .orElse(BigDecimal.ZERO);

        BigDecimal campaignDiscount = Optional.ofNullable(order.getCampaignDiscount())
                .orElse(BigDecimal.ZERO);

        BigDecimal amount = order.getSubtotal()
                .subtract(promoDiscount)
                .subtract(campaignDiscount);

        BigDecimal totalAmount = amount.add(order.getNetDeposit());

        order.setAmount(amount);
        order.setTotalAmount(totalAmount);

        log.info("Order {} final calculation - DepositCharged: {}, DepositRefunded: {}, NetDeposit: {}, TotalAmount: {}",
                order.getOrderNumber(),
                order.getTotalDepositCharged(),
                order.getTotalDepositRefunded(),
                order.getNetDeposit(),
                totalAmount);
    }
}