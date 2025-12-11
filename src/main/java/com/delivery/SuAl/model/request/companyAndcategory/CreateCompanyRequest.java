package com.delivery.SuAl.model.request.companyAndcategory;

import com.delivery.SuAl.model.CompanyStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    private CompanyStatus companyStatus;
}
