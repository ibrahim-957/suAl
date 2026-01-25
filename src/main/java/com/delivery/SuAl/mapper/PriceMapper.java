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

    @Mapping(target = "productId", source = "product.id")
    PriceResponse toResponse(Price price);

    List<PriceResponse> toResponseList(List<Price> prices);
}
