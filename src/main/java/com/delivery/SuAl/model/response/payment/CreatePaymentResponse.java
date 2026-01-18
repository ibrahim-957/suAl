package com.delivery.SuAl.model.response.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
