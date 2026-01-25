package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Customer;
import com.delivery.SuAl.model.request.customer.CreateCustomerRequest;
import com.delivery.SuAl.model.request.customer.UpdateCustomerRequest;
import com.delivery.SuAl.model.response.customer.CustomerResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = {CustomerContainerMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CustomerMapper {
    Customer toEntity(CreateCustomerRequest createCustomerRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "addresses", ignore = true)
    @Mapping(target = "orders", ignore = true)
    @Mapping(target = "customerContainers", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    void updateEntityFromRequest(UpdateCustomerRequest updateCustomerRequest, @MappingTarget Customer customer);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.phoneNumber", target = "phoneNumber")
    @Mapping(source = "customerContainers", target = "customerContainers")
    CustomerResponse toResponse(Customer customer);
}
