package com.delivery.SuAl.model.request.affordablepackage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class CreateAffordablePackageRequest {

    @NotBlank(message = "Package name is required")
    private String name;

    private String description;

    @NotNull(message = "Total price is required")
    @DecimalMin(value = "0.01", message = "Total price must be greater than 0")
    private BigDecimal totalPrice;

    @NotNull(message = "Max frequency is required")
    @Min(value = 1, message = "Max frequency must be at least 1")
    @Max(value = 12, message = "Max frequency cannot exceed 12")
    private Integer maxFrequency;

    private Long companyId;

    @NotEmpty(message = "Package must contain at least one product")
    @Valid
    private List<PackageProductRequest> products;
}
