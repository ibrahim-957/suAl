package com.delivery.SuAl.model.request.affordablepackage;

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
public class UpdateAffordablePackageRequest {
    private String name;
    private String description;
    private BigDecimal totalPrice;
    private Boolean isActive;
    private List<PackageProductRequest> products;
}
