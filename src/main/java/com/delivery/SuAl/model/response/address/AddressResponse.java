package com.delivery.SuAl.model.response.address;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddressResponse {
    private Long id;
    private String description;
    private String city;
    private String street;
    private String buildingNumber;
    private String apartmentNumber;
    private String postalCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String fullAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
