package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Price;
import com.delivery.SuAl.model.request.product.CreatePriceRequest;
import com.delivery.SuAl.model.request.product.UpdatePriceRequest;
import com.delivery.SuAl.model.response.product.PriceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PriceMapper {
    Price toEntity(CreatePriceRequest createPriceRequest);

    void updateEntityFromRequest(UpdatePriceRequest updatePriceRequest,
                                 @MappingTarget Price price);

    @Mapping(target = "productId", source = "product.id")
    PriceResponse toResponse(Price price);

    List<PriceResponse> toResponseList(List<Price> prices);
}
