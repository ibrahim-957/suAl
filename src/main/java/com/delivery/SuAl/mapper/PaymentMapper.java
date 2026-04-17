package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Payment;
import com.delivery.SuAl.model.dto.payment.CreatePaymentDTO;
import com.delivery.SuAl.model.dto.payment.PaymentDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Mapper(componentModel = "spring", uses = {CurrencyMapper.class, DateTimeMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PaymentMapper {
    @Mapping(target = "amountInCoins", expression = "java(convertToCoins(dto.getAmount()))")
    @Mapping(target = "transactionType", source = "type", defaultValue = "SMS")
    @Mapping(target = "paymentProvider", constant = "MAGNET")
    @Mapping(target = "paymentMethod", constant = "CARD")
    @Mapping(
            target = "currencyCode",
            expression = "java(dto.getCurrencyCode() == null ? \"944\" : CurrencyMapper.toNumericCode(dto.getCurrencyCode()))"
    )
    @Mapping(target = "paymentStatus", constant = "CREATED")
    Payment toEntity(CreatePaymentDTO dto);

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "amount", expression = "java(payment.getAmountAsDecimal())")
    @Mapping(target = "createdAt", qualifiedByName = "utcToBaku")
    PaymentDTO toDto(Payment payment);

    default Long convertToCoins(BigDecimal amount) {
        if (amount == null) return null;

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }

        BigDecimal coins = amount
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP);

        if (coins.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0) {
            throw new IllegalArgumentException("Amount too large to convert to coins");
        }

        return coins.longValue();
    }

}