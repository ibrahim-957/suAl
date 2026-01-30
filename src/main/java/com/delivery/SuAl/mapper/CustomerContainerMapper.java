package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.CustomerContainer;
import com.delivery.SuAl.model.response.customer.CustomerContainerResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CustomerContainerMapper {
    @Mappings({
            @Mapping(target = "productId", source = "product.id"),
            @Mapping(target = "productName", source = "product.name"),
            @Mapping(target = "productSize", source = "product.size"),
            @Mapping(target = "companyName", source = "product.company.name"),
            @Mapping(target = "depositAmount", source = "product.depositAmount")
    })
    CustomerContainerResponse toResponse(CustomerContainer customerContainer);
}