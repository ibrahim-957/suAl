package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Price;
import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.model.request.product.CreateProductRequest;
import com.delivery.SuAl.model.request.product.UpdateProductRequest;
import com.delivery.SuAl.model.response.product.ProductResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "category", ignore = true)
    Product toEntity(CreateProductRequest createProductRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    void updateEntityFromRequest(UpdateProductRequest updateProductRequest,
                                 @MappingTarget Product product);

    @Mapping(target = "companyName", expression = "java(getCompanyName(product))")
    @Mapping(target = "categoryType", expression = "java(getCategoryType(product))")
    @Mapping(target = "sellPrice", expression = "java(getCurrentSellPrice(product))")
    @Mapping(target = "buyPrice", expression = "java(getCurrentBuyPrice(product))")
    ProductResponse toResponse(Product product);

    List<ProductResponse> toResponseList(List<Product> products);

    default String getCompanyName(Product product) {
        if (product.getCompany() == null) {
            return null;
        }
        return product.getCompany().getName();
    }

    default com.delivery.SuAl.model.enums.CategoryType getCategoryType(Product product) {
        if (product.getCategory() == null) {
            return null;
        }
        return product.getCategory().getCategoryType();
    }

    default BigDecimal getCurrentSellPrice(Product product) {
        if (product.getPrices() == null || product.getPrices().isEmpty()) {
            return BigDecimal.ZERO;
        }
        Price latestPrice = product.getPrices().get(product.getPrices().size() - 1);
        return latestPrice.getSellPrice();
    }

    default BigDecimal getCurrentBuyPrice(Product product) {
        if (product.getPrices() == null || product.getPrices().isEmpty()) {
            return BigDecimal.ZERO;
        }
        Price latestPrice = product.getPrices().get(product.getPrices().size() - 1);
        return latestPrice.getBuyPrice();
    }
}