package com.delivery.SuAl.model.request.affordablepackage;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PackageProductRequest {
    @NotNull
    private Long productId;

    @NotNull
    @Min(value = 1)
    private Integer quantity;
}
