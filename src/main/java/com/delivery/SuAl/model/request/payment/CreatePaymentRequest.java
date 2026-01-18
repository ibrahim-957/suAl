package com.delivery.SuAl.model.request.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreatePaymentRequest {
    private String reference;
    private String type;
    private String token;
    private String save;
    private Long amount;
    private String currency;
    private String biller;
    private String description;
    private String template;
    private String language;
    private String callback;
    private String extra;
}