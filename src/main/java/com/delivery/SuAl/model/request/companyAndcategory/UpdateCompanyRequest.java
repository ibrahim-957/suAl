package com.delivery.SuAl.model.request.companyAndcategory;

import com.delivery.SuAl.model.enums.CompanyStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCompanyRequest {
    private String name;

    private String description;

    @NotNull
    private CompanyStatus companyStatus;
}
