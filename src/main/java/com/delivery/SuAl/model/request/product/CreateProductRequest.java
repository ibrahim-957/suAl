package com.delivery.SuAl.model.request.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request object for creating a new product")
public class CreateProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 255)
    private String name;

    @NotNull
    private Long companyId;

    @NotNull
    private Long categoryId;

    @NotNull
    private Long sizeId;

    @NotNull
    private Long warehouseId;

    @DecimalMin(value = "0.0", message = "Deposit amount cannot be negative")
    private BigDecimal depositAmount;

    @Size(max = 2000)
    private String description;

    @NotNull(message = "Mineral composition is required")
    private Map<String, String> mineralComposition;

    @Schema(description = "Whether the container must be returned")
    private boolean returnable;

    @Min(value = 1)
    private Integer minimumStockAlert;
}