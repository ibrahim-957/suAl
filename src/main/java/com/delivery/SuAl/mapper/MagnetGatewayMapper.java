package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Payment;
import com.delivery.SuAl.model.enums.PaymentMethod;
import com.delivery.SuAl.model.enums.PaymentStatus;
import com.delivery.SuAl.model.response.payment.PaymentStatusResponse;
import com.delivery.SuAl.model.response.payment.RefundDetails;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Mapper(componentModel = "spring")
public interface MagnetGatewayMapper {
    @Mapping(target = "amountInCoins", expression = "java(mapAmountToCoins(response.getAmount()))")
    @Mapping(target = "currencyCode", source = "currency")
    @Mapping(target = "paymentMethod", expression = "java(mapPaymentMethod(response.getMethod()))")
    @Mapping(target = "gatewayStatusCode", source = "status")
    @Mapping(target = "gatewayResponseCode", source = "code")
    @Mapping(target = "gatewayMessage", source = "message")
    @Mapping(target = "paymentDatetime", expression = "java(parseDateTime(response.getDatetime()))")
    @Mapping(target = "paymentStatus", expression = "java(mapGatewayStatus(response.getStatus()))")
    @Mapping(target = "paidAt", expression = "java(mapPaidAt(response.getStatus(), response.getDatetime()))")
    @Mapping(target = "refundAmountInCoins", expression = "java(mapRefundAmount(response.getRefund()))")
    @Mapping(target = "refundedAt", expression = "java(mapRefundedAt(response.getRefund()))")
    @Mapping(target = "failureReason", expression = "java(mapFailureReason(response))")
    void updatePaymentFromStatusResponse(PaymentStatusResponse response, @MappingTarget Payment payment);

    default Long mapAmountToCoins(Double amount) {
        if (amount == null) return null;
        return Math.round(amount * 100);
    }

    default Long mapRefundAmount(List<RefundDetails> refundList) {
        if (refundList == null || refundList.isEmpty()) {
            return null;
        }
        return refundList.stream()
                .filter(refund -> refund.getAmount() != null)
                .mapToLong(refund -> Math.round(refund.getAmount() * 100))
                .sum();
    }

    default LocalDateTime mapRefundedAt(List<RefundDetails> refundList) {
        if (refundList == null || refundList.isEmpty()) {
            return null;
        }
        RefundDetails latestRefund = refundList.getLast();
        if (latestRefund.getDatetime() != null) {
            return parseDateTime(latestRefund.getDatetime());
        }
        return null;
    }

    default PaymentMethod mapPaymentMethod(String method) {
        if (method == null) return PaymentMethod.CARD;
        return switch (method.toUpperCase()) {
            case "GOOGLEPAY" -> PaymentMethod.GOOGLE_PAY;
            case "APPLEPAY" -> PaymentMethod.APPLE_PAY;
            default -> PaymentMethod.CARD;
        };
    }

    default PaymentStatus mapGatewayStatus(String status) {
        if (status == null) return PaymentStatus.ERROR;

        return switch (status) {
            case "00" -> PaymentStatus.SUCCESS;
            case "S0", "S1", "S2" -> PaymentStatus.PENDING;
            case "S4" -> PaymentStatus.CANCELLED;
            default -> PaymentStatus.FAILED;
        };
    }

    default LocalDateTime parseDateTime(String datetime) {
        if (datetime == null) return null;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
            return LocalDateTime.parse(datetime, formatter);
        } catch (Exception e) {
            return null;
        }
    }

    default LocalDateTime mapPaidAt(String status, String datetime) {
        if ("00".equals(status)) {
            return parseDateTime(datetime);
        }
        return null;
    }

    default String mapFailureReason(PaymentStatusResponse response) {
        if (response == null) return "Unknown gateway error";

        return "00".equals(response.getStatus())
                ? null
                : response.getMessage();
    }
}