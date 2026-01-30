package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Price;
import com.delivery.SuAl.model.request.product.CreatePriceRequest;
import com.delivery.SuAl.model.request.product.UpdatePriceRequest;
import com.delivery.SuAl.model.response.product.PriceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PriceMapper {
    Price toEntity(CreatePriceRequest createPriceRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(UpdatePriceRequest updatePriceRequest,
                                 @MappingTarget Price price);

    @Mapping(target = "productId", expression = "java(getProductId(price))")
    @Mapping(target = "productName", expression = "java(getProductName(price))")
    @Mapping(target = "companyName", expression = "java(getCompanyName(price))")
    @Mapping(target = "categoryType", expression = "java(getCategoryType(price))")
    PriceResponse toResponse(Price price);

    List<PriceResponse> toResponseList(List<Price> prices);

    default Long getProductId(Price price) {
        if (price.getProduct() == null) {
            return null;
        }
        return price.getProduct().getId();
    }

    default String getProductName(Price price) {
        if (price.getProduct() == null) {
            return null;
        }
        return price.getProduct().getName();
    }

    default String getCompanyName(Price price) {
        if (price.getProduct() == null || price.getProduct().getCompany() == null) {
            return null;
        }
        return price.getProduct().getCompany().getName();
    }

    default com.delivery.SuAl.model.enums.CategoryType getCategoryType(Price price) {
        if (price.getProduct() == null || price.getProduct().getCategory() == null) {
            return null;
        }
        return price.getProduct().getCategory().getCategoryType();
    }
}