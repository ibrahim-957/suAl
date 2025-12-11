package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Category;
import com.delivery.SuAl.model.request.companyAndcategory.CreateCategoryRequest;
import com.delivery.SuAl.model.request.companyAndcategory.UpdateCategoryRequest;
import com.delivery.SuAl.model.response.companyAndcategory.CategoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    @Mapping(target = "categoryType", source = "categoryType")
    Category toEntity(CreateCategoryRequest createCategoryRequest);

    void updateEntityFromRequest(UpdateCategoryRequest updateCategoryRequest,
                                 @MappingTarget Category category);

    //@Mapping(target = "productCount", ignore = true)
    CategoryResponse toResponse(Category category);

    //List<CategoryResponse> toResponseList(List<Category> categoryList);
}
