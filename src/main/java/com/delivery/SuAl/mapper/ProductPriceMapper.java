package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.ProductPrice;
import com.delivery.SuAl.model.request.product.CreateProductPriceRequest;
import com.delivery.SuAl.model.response.product.ProductPriceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Mapper(componentModel = "spring", uses = {DateTimeMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProductPriceMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "validFrom", ignore = true)
    @Mapping(target = "validTo", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    ProductPrice toEntity(CreateProductPriceRequest request);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "effectivePrice", expression = "java(calculateEffectivePrice(productPrice))")
    @Mapping(target = "createdBy", expression = "java(getCreatedByName(productPrice))")
    @Mapping(target = "validFrom", qualifiedByName = "utcToBaku")
    @Mapping(target = "validTo", qualifiedByName = "utcToBaku")
    @Mapping(target = "createdAt", qualifiedByName = "utcToBaku")
    ProductPriceResponse toResponse(ProductPrice productPrice);

    List<ProductPriceResponse> toResponseList(List<ProductPrice> prices);

    default BigDecimal calculateEffectivePrice(ProductPrice productPrice) {
        if (productPrice.getSellPrice() == null) return null;
        if (productPrice.getDiscountPercent() == null ||
                productPrice.getDiscountPercent().compareTo(BigDecimal.ZERO) == 0) {
            return productPrice.getSellPrice();
        }
        BigDecimal multiplier = BigDecimal.ONE
                .subtract(productPrice.getDiscountPercent()
                        .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
        return productPrice.getSellPrice()
                .multiply(multiplier)
                .setScale(2, RoundingMode.HALF_UP);
    }

    default String getCreatedByName(ProductPrice price) {
        if (price.getCreatedBy() == null) return null;
        return price.getCreatedBy().getEmail() != null
                ? price.getCreatedBy().getEmail()
                : price.getCreatedBy().getPhoneNumber();
    }
}
