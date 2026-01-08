package com.delivery.SuAl.model.request.companyAndcategory;

import com.delivery.SuAl.model.enums.CategoryType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCategoryRequest {
    public CategoryType categoryType;
}
