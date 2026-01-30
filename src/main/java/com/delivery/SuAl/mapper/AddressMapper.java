package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Address;
import com.delivery.SuAl.model.request.address.CreateAddressByCustomerRequest;
import com.delivery.SuAl.model.request.address.CreateAddressByOperatorRequest;
import com.delivery.SuAl.model.request.address.UpdateAddressRequest;
import com.delivery.SuAl.model.response.address.AddressResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AddressMapper {
    Address toEntity(CreateAddressByOperatorRequest createAddressByOperatorRequest);

    Address toEntity(CreateAddressByCustomerRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(UpdateAddressRequest updateAddressRequest,
                                 @MappingTarget Address address);

    AddressResponse toResponse(Address address);

    List<AddressResponse> toResponseList(List<Address> addresses);

}
