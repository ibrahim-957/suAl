package com.delivery.SuAl.service;

import com.delivery.SuAl.model.request.address.CreateAddressRequest;
import com.delivery.SuAl.model.request.address.UpdateAddressRequest;
import com.delivery.SuAl.model.response.address.AddressResponse;

import java.util.List;

public interface AddressService {
    AddressResponse createAddressByCustomer(String phoneNumber, CreateAddressRequest request);

    AddressResponse createAddress(Long customerId, CreateAddressRequest createAddressRequest);

    AddressResponse getAddressById(Long customerId, Long addressId);

    AddressResponse updateAddress(Long customerId, Long addressId, UpdateAddressRequest updateAddressRequest);

    void deleteAddress(Long id);

    List<AddressResponse> getCustomerAddresses(Long customerId);
}
