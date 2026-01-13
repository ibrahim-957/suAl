package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.model.request.product.CreateProductRequest;
import com.delivery.SuAl.model.request.product.UpdateProductRequest;
import com.delivery.SuAl.model.response.product.ProductResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    Product toEntity(CreateProductRequest createProductRequest);

    void updateEntityFromRequest(UpdateProductRequest updateProductRequest,
                                 @MappingTarget Product product);

    @Mapping(target = "companyName", source = "company.name")
    @Mapping(target = "categoryType", source = "category.categoryType")
    ProductResponse toResponse(Product product);

    List<ProductResponse> toResponseList(List<Product> products);
}
