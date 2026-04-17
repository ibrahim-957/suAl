package com.delivery.SuAl.model.response.companyAndcategory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CategoryResponse {
    private Long id;
    private String name;
    private Boolean isActive;
    private Long productCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
