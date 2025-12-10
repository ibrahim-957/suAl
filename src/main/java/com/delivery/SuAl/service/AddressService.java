package com.delivery.SuAl.service;

import com.delivery.SuAl.model.request.address.CreateAddressRequest;
import com.delivery.SuAl.model.request.address.UpdateAddressRequest;
import com.delivery.SuAl.model.response.address.AddressResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import org.springframework.data.domain.Pageable;

public interface AddressService {
    AddressResponse createAddress(CreateAddressRequest createAddressRequest);

    AddressResponse getAddressById(Long id);

    AddressResponse updateAddress(Long id, UpdateAddressRequest updateAddressRequest);

    void deleteAddress(Long id);

    PageResponse<AddressResponse> getAllAddresses(Pageable pageable);
}
