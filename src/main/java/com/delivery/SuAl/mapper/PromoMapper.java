package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Promo;
import com.delivery.SuAl.model.request.marketing.CreatePromoRequest;
import com.delivery.SuAl.model.request.marketing.UpdatePromoRequest;
import com.delivery.SuAl.model.response.marketing.PromoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PromoMapper {
    Promo toEntity(CreatePromoRequest request);

    void updateEntityFromRequest(UpdatePromoRequest request, @MappingTarget Promo promo);

    PromoResponse toResponse(Promo promo);
}