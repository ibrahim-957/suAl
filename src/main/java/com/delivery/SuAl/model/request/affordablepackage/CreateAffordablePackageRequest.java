package com.delivery.SuAl.model.request.affordablepackage;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateAffordablePackageRequest {
    @NotBlank
    private String name;

    private String description;

    @NotNull
    @Min(value = 0)
    private BigDecimal totalPrice;

    private Long companyId;

    @NotEmpty
    private List<PackageProductRequest> products;
}
