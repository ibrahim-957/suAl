package com.delivery.SuAl.model.response.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentStatusResponse {
    private String reference;
    private String datetime;
    private String method;
    private String type;
    private String token;
    private String pan;
    private String expiry;
    private Double amount;
    private Double fee;
    private Double offset;
    private String currency;
    private String biller;
    private String system;
    private String issuer;
    private String rrn;
    private String approval;
    @JsonProperty("3ds")
    private String threeDsStatus;
    private List<RefundDetails> refund;
    private List<Object> extra;
    @JsonProperty("transactionList")
    private List<Object> transactionList;
    private String status;
    private Integer code;
    private String message;
}
