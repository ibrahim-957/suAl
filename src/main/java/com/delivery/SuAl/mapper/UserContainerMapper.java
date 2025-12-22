package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.UserContainer;
import com.delivery.SuAl.model.response.user.UserContainerResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface UserContainerMapper {
    @Mappings({
            @Mapping(target = "productId", source = "product.id"),
            @Mapping(target = "productName", source = "product.name"),
            @Mapping(target = "productSize", source = "product.size"),
            @Mapping(target = "companyName", source = "product.company.name"),
            @Mapping(target = "depositAmount", source = "product.depositAmount")
    })
    UserContainerResponse toResponse(UserContainer userContainer);

}
