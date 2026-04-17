package com.delivery.SuAl.model.request.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductSizeRequest {
    @NotBlank(message = "Size label is required")
    @Size(max = 50)
    private String label;

    private Boolean isActive;
}
