package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Payment;
import com.delivery.SuAl.model.dto.payment.CreatePaymentDTO;
import com.delivery.SuAl.model.dto.payment.PaymentDTO;
import com.delivery.SuAl.model.dto.payment.PaymentStatusDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Mapper(componentModel = "spring", uses = {CurrencyMapper.class})
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
    PaymentDTO toDto(Payment payment);

    List<PaymentDTO> toDtoList(List<Payment> payments);

    @Mapping(target = "amount", expression = "java(payment.getAmountAsDecimal())")
    PaymentStatusDTO toStatusDto(Payment payment);

    default Long convertToCoins(BigDecimal amount) {
        if (amount == null) return null;
        return amount
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

    }
}