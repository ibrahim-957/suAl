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

//    @Mapping(target = "fullAddress", expression = "java(buildFullAddress(address))")
    AddressResponse toResponse(Address address);

    List<AddressResponse> toResponseList(List<Address> addresses);

    default String buildFullAddress(Address address) {
        StringBuilder sb = new StringBuilder();
        if (address.getCity() != null) sb.append(address.getCity());
        if(address.getStreet() != null) sb.append(", ").append(address.getStreet());
        if (address.getBuildingNumber() != null) sb.append(", ").append(address.getBuildingNumber());
        if (address.getApartmentNumber() != null) sb.append(", ").append(address.getApartmentNumber());
        if (address.getPostalCode() != null) sb.append(", ").append(address.getPostalCode());
        return sb.toString().trim();

    }
}
