package com.delivery.SuAl.model.request.operation;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RejectOrderRequest {
    @NotBlank
    private String reason;
}
