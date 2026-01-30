package com.delivery.SuAl.model.request.address;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateAddressByOperatorRequest {
    @NotNull
    private Long customerId;
    @NotBlank
    private String description;

    @NotBlank
    private String city;

    @NotBlank
    private String street;

    private String buildingNumber;

    private String apartmentNumber;

    private String postalCode;

    @Digits(integer = 2, fraction = 8)
    private BigDecimal latitude;

    @Digits(integer = 3, fraction = 8)
    private BigDecimal longitude;
}
