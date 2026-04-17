package com.delivery.SuAl.model.response.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreatePaymentResponse {
    private Integer code;
    private String message;
    private String url;
    private String id;

    @JsonProperty("token")
    private String token;
}
