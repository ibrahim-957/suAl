package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Category;
import com.delivery.SuAl.model.request.companyAndcategory.CreateCategoryRequest;
import com.delivery.SuAl.model.request.companyAndcategory.UpdateCategoryRequest;
import com.delivery.SuAl.model.response.companyAndcategory.CategoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = {DateTimeMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CategoryMapper {
    @Mapping(target = "name", source = "name")
    Category toEntity(CreateCategoryRequest createCategoryRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(UpdateCategoryRequest updateCategoryRequest,
                                 @MappingTarget Category category);

    @Mapping(target = "createdAt", qualifiedByName = "utcToBaku")
    @Mapping(target = "updatedAt", qualifiedByName = "utcToBaku")
    CategoryResponse toResponse(Category category);
}
