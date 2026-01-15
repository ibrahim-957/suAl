package com.delivery.SuAl.model.request.product;

import com.delivery.SuAl.model.enums.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request object for updating an existing product")
public class UpdateProductRequest {

    @Schema(description = "Product name", example = "Dübrar Su Premium")
    @Size(min = 2, max = 255)
    private String name;

    @Schema(description = "Company ID", example = "1")
    private Long companyId;

    @Schema(description = "Category ID", example = "2")
    private Long categoryId;

    @Schema(description = "Product size", example = "19L")
    @Size(max = 50)
    private String size;

    @Schema(description = "Price paid to supplier", example = "4.50")
    private BigDecimal buyPrice;

    @Schema(description = "Price sold to customers", example = "6.00")
    private BigDecimal sellPrice;

    @Schema(description = "Detailed description", example = "Updated source information...")
    @Size(max = 2000)
    private String description;

    @Schema(
            description = "Mineral composition map",
            example = "{\"Mg\":\"<10\",\"Na\":\"<12\",\"pH\":\"7.5\"}"
    )
    private Map<String, String> mineralComposition;

    @Schema(description = "Deposit amount for the bottle", example = "15.00")
    @DecimalMin(value = "0.0", inclusive = true, message = "Deposit amount cannot be negative")
    @DecimalMax(value = "999.99", message = "Deposit amount is too large")
    @Digits(integer = 3, fraction = 2, message = "Deposit amount must have at most 3 digits before decimal and 2 after")
    private BigDecimal depositAmount;

    @Schema(description = "Current status of the product", example = "ACTIVE")
    private ProductStatus productStatus;
}