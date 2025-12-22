package com.delivery.SuAl.model.request.companyAndcategory;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCompanyRequest {
    @NotBlank
    private String name;

    private String description;
}
