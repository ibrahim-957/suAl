package com.delivery.SuAl.model.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerifyOtpRequest {
    @NotBlank
    private String phoneNumber;

    @NotBlank
    private String code;
}
