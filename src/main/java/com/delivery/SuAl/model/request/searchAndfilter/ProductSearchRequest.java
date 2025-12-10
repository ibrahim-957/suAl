package com.delivery.SuAl.model.request.searchAndfilter;

import com.delivery.SuAl.model.ProductStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductSearchRequest {
    private String name;
    private Long companyId;
    private Long categoryId;
    private String size;
    private ProductStatus productStatus;

    @DecimalMin(value = "0.0", message = "Minimum price cannot be negative")
    private BigDecimal minPrice;

    @DecimalMax(value = "0.0", message = "Maximum price cannot be negative")
    private BigDecimal maxPrice;

    @Min(value = 0, message = "Page number cannot be negative")
    private Integer page = 0;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    private Integer pageSize = 20;

    private String sortBy = "name";
    private String sortDirection = "ASC";
}
