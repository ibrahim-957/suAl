package com.delivery.SuAl.model.response.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentStatusResponse {
    private String reference;
    private String datetime;
    private String method;
    private String type;
    private String token;
    private String pan;
    private String expiry;
    private Long amount;
    private Long fee;
    private Long offset;
    private String currency;
    private String biller;
    private String system;
    private String issuer;
    private String rrn;
    private String approval;
    @JsonProperty("3ds")
    private String threeDsStatus;
    private RefundDetails refund;
    private String status;
    private String code;
    private String message;
    private String extra;
}
