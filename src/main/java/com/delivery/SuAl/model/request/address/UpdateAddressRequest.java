package com.delivery.SuAl.model.request.address;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateAddressRequest {
    private String description;

    private String city;

    private String street;

    private String buildingNumber;

    private String apartmentNumber;

    private String postalCode;

    private BigDecimal  latitude;

    private BigDecimal longitude;
}
