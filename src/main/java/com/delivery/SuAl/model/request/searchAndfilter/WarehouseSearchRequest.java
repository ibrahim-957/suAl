package com.delivery.SuAl.model.request.searchAndfilter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WarehouseSearchRequest {
    private Long productId;

    private Long companyId;

    private Long categoryId;

    private Boolean lowStockOnly;

    private Boolean outOfStockOnly;

    private Boolean hasEmptiesOnly;

    @Min(value = 0)
    private Integer minFullCount;

    @Min(value = 0)
    private Integer maxFullCount;

    @Min(value = 0)
    private Integer page = 0;

    @Min(value = 1)
    @Max(value = 100)
    private Integer pageSize = 20;

    private String sortBy = "fullCount";
    private String sortDirection = "ASC";
}
