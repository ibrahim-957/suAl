package com.delivery.SuAl.model.response.companyAndcategory;

import com.delivery.SuAl.model.enums.CompanyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponse {
    private Long id;
    private String name;
    private String description;
    private CompanyStatus companyStatus;
    private Long productCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
