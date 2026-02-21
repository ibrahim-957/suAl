package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.model.request.product.CreateProductRequest;
import com.delivery.SuAl.model.request.product.UpdateProductRequest;
import com.delivery.SuAl.model.response.product.ProductResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", uses = {DateTimeMapper.class, ProductSizeMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "size", ignore = true)
    Product toEntity(CreateProductRequest createProductRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "size", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    void updateEntityFromRequest(UpdateProductRequest updateProductRequest,
                                 @MappingTarget Product product);

    @Mapping(target = "companyName", expression = "java(getCompanyName(product))")
    @Mapping(target = "categoryName", expression = "java(getCategoryName(product))")
    @Mapping(target = "size", source = "size")
    @Mapping(target = "sellPrice", ignore = true)
    @Mapping(target = "discountPercent", ignore = true)
    @Mapping(target = "effectivePrice", ignore = true)
    @Mapping(target = "createdAt", qualifiedByName = "utcToBaku")
    @Mapping(target = "updatedAt", qualifiedByName = "utcToBaku")
    ProductResponse toResponse(Product product);

    List<ProductResponse> toResponseList(List<Product> products);

    default String getCompanyName(Product product) {
        if (product.getCompany() == null) return null;
        return product.getCompany().getName();
    }

    default String getCategoryName(Product product) {
        if (product.getCategory() == null) return null;
        return product.getCategory().getName();
    }
}