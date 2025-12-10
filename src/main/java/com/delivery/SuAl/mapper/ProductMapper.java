package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.model.request.product.CreateProductRequest;
import com.delivery.SuAl.model.request.product.UpdateProductRequest;
import com.delivery.SuAl.model.response.product.ProductResponse;
import com.delivery.SuAl.model.response.product.ProductSummaryResponse;
import com.delivery.SuAl.model.response.search.ProductSearchResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {
//    @Mapping(target = "company", ignore = true)
//    @Mapping(target = "category", ignore = true)
    Product toEntity(CreateProductRequest createProductRequest);

//    @Mapping(target = "company", ignore = true)
//    @Mapping(target = "category", ignore = true)
    void updateEntityFromRequest(UpdateProductRequest updateProductRequest,
                                 @MappingTarget Product product);

//    @Mapping(target = "companyName", source = "company.name")
//    @Mapping(target = "categoryType", source = "category.categoryType")
//    @Mapping(target = "sellPrice", ignore = true)
//    @Mapping(target = "buyPrice", ignore = true)
//    @Mapping(target = "profitMargin", ignore = true)
    ProductResponse toResponse(Product product);

//    @Mapping(target = "companyName", source = "company.name")
//    @Mapping(target = "sellPrice", ignore = true)
    ProductSummaryResponse toSummaryResponse(Product product);

//    @Mapping(target = "companyName", source = "company.name")
//    @Mapping(target = "sellPrice", ignore = true)
//    @Mapping(target = "stockCount", ignore = true)
    ProductSearchResponse toSearchResponse(Product product);

    List<ProductResponse> toResponseList(List<Product> products);

    List<ProductSummaryResponse> toSummaryResponseList(List<Product> products);

    List<ProductSearchResponse> toSearchResponseList(List<Product> products);
}
