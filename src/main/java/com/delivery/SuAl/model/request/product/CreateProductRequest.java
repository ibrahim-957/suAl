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

    @Schema(description = "Product name", example = "Dübrar Su", required = true)
    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 255)
    private String name;

    @Schema(description = "Company ID", example = "1", required = true)
    @NotNull
    private Long companyId;

    @Schema(description = "Category ID", example = "2", required = true)
    @NotNull
    private Long categoryId;

    @Schema(description = "Warehouse ID", example = "1", required = true)
    @NotNull
    private Long warehouseId;

    @Schema(description = "Product size", example = "19L", required = true)
    @Size(max = 50)
    private String size;

    @Schema(description = "Deposit amount", example = "10.00", required = true)
    @DecimalMin(value = "0.0", inclusive = true, message = "Deposit amount cannot be negative")
    @DecimalMax(value = "999.99", message = "Deposit amount is too large")
    @Digits(integer = 3, fraction = 2, message = "Deposit amount must have at most 3 digits before decimal and 2 after")
    private BigDecimal depositAmount;

    @Schema(description = "Buy price", example = "4", required = true)
    private BigDecimal buyPrice;

    @Schema(description = "Sell price", example = "5", required = true)
    private BigDecimal sellPrice;

    @Schema(description = "Product description", example = "Xızı rayonu ərazisində yerləşən təbii mineral su", required = true)
    @Size(max = 2000)
    private String description;

    @Schema(
            description = "Mineral composition (send as JSON string)",
            example = "{\"Mg\":\"<15\",\"Na\":\"<15\",\"SO4\":\"<15\",\"pH\":\"6-9\"}",
            required = true,
            type = "string"
    )
    @NotNull(message = "Mineral composition is required")
    private Map<String, String> mineralComposition;

    @Schema(description = "Initial full bottle count", example = "100", required = true)
    @NotNull
    @Min(value = 0)
    private Integer initialFullCount;

    @Schema(description = "Initial empty bottle count", example = "0", required = true)
    @NotNull
    @Min(value = 0)
    private Integer initialEmptyCount;

    @Schema(description = "Initial damaged bottle count", example = "0", required = true)
    @NotNull
    @Min(value = 0)
    private Integer initialDamagedCount;

    @Schema(description = "Minimum stock alert threshold", example = "10", required = true)
    @Min(value = 1)
    private Integer minimumStockAlert;
}