package com.delivery.SuAl.model.response.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MagnetResponse <T>{
    @JsonProperty("response")
    private T response;
}
