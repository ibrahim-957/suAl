package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Promo;
import com.delivery.SuAl.model.request.marketing.CreatePromoRequest;
import com.delivery.SuAl.model.request.marketing.UpdatePromoRequest;
import com.delivery.SuAl.model.response.marketing.PromoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PromoMapper {
    Promo toEntity(CreatePromoRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(UpdatePromoRequest request, @MappingTarget Promo promo);

    @Mapping(target = "maxUsesPerUser", source = "maxUsesPerCustomer")
    @Mapping(target = "usageRemaining", expression = "java(calculateUsageRemaining(promo))")
    PromoResponse toResponse(Promo promo);

    default Integer calculateUsageRemaining(Promo promo) {
        if (promo.getMaxTotalUses() == null) {
            return null;
        }
        return Math.max(0, promo.getMaxTotalUses() - promo.getCurrentTotalUses());
    }
}