package com.delivery.SuAl.model.request.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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

    @Schema(description = "Product name", required = true, example = "Spring Water 5L")
    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 255)
    private String name;

    @Schema(description = "Company ID", required = true, example = "1")
    @NotNull
    private Long companyId;

    @Schema(description = "Category ID", required = true, example = "2")
    @NotNull
    private Long categoryId;

    @Schema(description = "Warehouse ID", required = true, example = "3")
    @NotNull
    private Long warehouseId;

    @Schema(description = "Product size", required = true, example = "5L")
    @Size(max = 50)
    private String size;

    @Schema(description = "Deposit amount", required = true, example = "10.50")
    @DecimalMin(value = "0.0", inclusive = true, message = "Deposit amount cannot be negative")
    @DecimalMax(value = "999.99", message = "Deposit amount is too large")
    @Digits(integer = 3, fraction = 2, message = "Deposit amount must have at most 3 digits before decimal and 2 after")
    private BigDecimal depositAmount;

    @Schema(description = "Buy price", required = true, example = "5.00")
    private BigDecimal buyPrice;

    @Schema(description = "Sell price", required = true, example = "8.00")
    private BigDecimal sellPrice;

    @Schema(description = "Product description", required = true, example = "Fresh spring water")
    @Size(max = 2000)
    private String description;

    @Schema(description = "Mineral composition (JSON string)",
            required = true,
            example = "{\"calcium\":\"50mg\",\"magnesium\":\"20mg\"}")
    @NotNull(message = "Mineral composition is required")
    private Map<String, String> mineralComposition;

    @Schema(description = "Initial full bottle count", required = true, example = "100")
    @NotNull
    @Min(value = 0)
    private Integer initialFullCount;

    @Schema(description = "Initial empty bottle count", required = true, example = "0")
    @NotNull
    @Min(value = 0)
    private Integer initialEmptyCount;

    @Schema(description = "Initial damaged bottle count", required = true, example = "0")
    @NotNull
    @Min(value = 0)
    private Integer initialDamagedCount;

    @Schema(description = "Minimum stock alert threshold", required = true, example = "10")
    @Min(value = 1)
    private Integer minimumStockAlert;
}