package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.ProductSize;
import com.delivery.SuAl.model.response.product.ProductSizeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProductSizeMapper {
    ProductSizeResponse toResponse(ProductSize productSize);
    List<ProductSizeResponse> toResponseList(List<ProductSize> sizes);
}
