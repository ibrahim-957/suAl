package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Address;
import com.delivery.SuAl.model.request.address.CreateAddressRequest;
import com.delivery.SuAl.model.request.address.UpdateAddressRequest;
import com.delivery.SuAl.model.response.address.AddressResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AddressMapper {
    Address toEntity(CreateAddressRequest createAddressRequest);

    void updateEntityFromRequest(UpdateAddressRequest updateAddressRequest,
                                 @MappingTarget Address address);

    AddressResponse toResponse(Address address);

    List<AddressResponse> toResponseList(List<Address> addresses);

}
