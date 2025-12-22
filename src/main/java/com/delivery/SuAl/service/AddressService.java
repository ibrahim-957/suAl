package com.delivery.SuAl.service;

import com.delivery.SuAl.model.request.address.CreateAddressRequest;
import com.delivery.SuAl.model.request.address.UpdateAddressRequest;
import com.delivery.SuAl.model.response.address.AddressResponse;

import java.util.List;

public interface AddressService {
    AddressResponse createAddress(Long userId, CreateAddressRequest createAddressRequest);

    AddressResponse getAddressById(Long userId, Long addressId);

    AddressResponse updateAddress(Long userId, Long addressId, UpdateAddressRequest updateAddressRequest);

    void deleteAddress(Long id);

    List<AddressResponse> getUserAddresses(Long userId);
}
