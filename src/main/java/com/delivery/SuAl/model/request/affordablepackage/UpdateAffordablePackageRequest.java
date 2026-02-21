package com.delivery.SuAl.model.request.affordablepackage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAffordablePackageRequest {

    private String name;

    private String description;

    @DecimalMin(value = "0.01", message = "Total price must be greater than 0")
    private BigDecimal totalPrice;

    @Min(value = 1, message = "Max frequency must be at least 1")
    @Max(value = 12, message = "Max frequency cannot exceed 12")
    private Integer maxFrequency;

    private Boolean isActive;

    @Valid
    private List<PackageProductRequest> products;
}
