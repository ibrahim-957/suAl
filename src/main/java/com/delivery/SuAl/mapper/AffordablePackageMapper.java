package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.AffordablePackage;
import com.delivery.SuAl.entity.AffordablePackageProduct;
import com.delivery.SuAl.entity.Company;
import com.delivery.SuAl.model.response.affordablepackage.AffordablePackageResponse;
import com.delivery.SuAl.model.response.affordablepackage.PackageProductResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring",
        uses = {DateTimeMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AffordablePackageMapper {

    @Mapping(target = "companyId", expression = "java(getCompanyId(affordablePackage.getCompany()))")
    @Mapping(target = "companyName", expression = "java(getCompanyName(affordablePackage.getCompany()))")
    @Mapping(target = "totalContainers", expression = "java(affordablePackage.getTotalContainers())")
    @Mapping(target = "products", expression = "java(mapProducts(affordablePackage.getPackageProducts()))")
    @Mapping(target = "createdAt", qualifiedByName = "utcToBaku")
    @Mapping(target = "updatedAt", qualifiedByName = "utcToBaku")
    @Mapping(target = "frequencyDescription", expression = "java(getFrequencyDescription(affordablePackage))")
    @Mapping(target = "originalValue", ignore = true)
    @Mapping(target = "profit", ignore = true)
    AffordablePackageResponse toResponse(AffordablePackage affordablePackage);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "pricePerUnit", ignore = true)
    @Mapping(target = "depositPerUnit", source = "product.depositAmount")
    @Mapping(target = "imageUrl", source = "product.imageUrl")
    @Mapping(target = "lineOriginalValue", ignore = true)
    PackageProductResponse toProductResponse(AffordablePackageProduct packageProduct);

    default List<PackageProductResponse> mapProducts(List<AffordablePackageProduct> packageProducts) {
        if (packageProducts == null) return new ArrayList<>();
        return packageProducts.stream()
                .map(this::toProductResponse)
                .collect(Collectors.toList());
    }

    default Long getCompanyId(Company company) {
        return company == null ? null : company.getId();
    }

    default String getCompanyName(Company company) {
        return company == null ? null : company.getName();
    }

    default String getFrequencyDescription(AffordablePackage affordablePackage) {
        Integer max = affordablePackage.getMaxFrequency();
        if (max == null || max <= 0) return "Unlimited deliveries";
        if (max == 1) return "Single delivery only";
        return String.format("1-%d deliveries allowed", max);
    }
}