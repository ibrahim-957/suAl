package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Address;
import com.delivery.SuAl.mapper.AddressMapper;
import com.delivery.SuAl.model.request.address.CreateAddressRequest;
import com.delivery.SuAl.model.request.address.UpdateAddressRequest;
import com.delivery.SuAl.model.response.address.AddressResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {
    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;

    @Override
    @Transactional
    public AddressResponse createAddress(CreateAddressRequest createAddressRequest) {
        log.info("Creating new address in city: {}", createAddressRequest.getCity());

        Address address = addressMapper.toEntity(createAddressRequest);
        Address savedAddress = addressRepository.save(address);

        log.info("Address created successfully with ID: {}", savedAddress.getId());
        return addressMapper.toResponse(savedAddress);
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getAddressById(Long id) {
        log.info("Getting address by ID: {}", id);

        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found with ID: " + id));
        return addressMapper.toResponse(address);
    }

    @Override
    public AddressResponse updateAddress(Long id, UpdateAddressRequest updateAddressRequest) {
        log.info("Updating address with ID: {}", id);

        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found with ID: " + id));

        addressMapper.updateEntityFromRequest(updateAddressRequest, address);
        Address updatedAddress = addressRepository.save(address);

        log.info("Address updated successfully with ID: {}", id);
        return addressMapper.toResponse(updatedAddress);
    }

    @Override
    @Transactional
    public void deleteAddress(Long id) {
        addressRepository.deleteById(id);
        log.info("Address deleted successfully with ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AddressResponse> getAllAddresses(Pageable pageable) {
        log.info("Getting all addresses with pagination");

        Page<Address> addressPage = addressRepository.findAll(pageable);
        List<AddressResponse> addressResponses = addressMapper.toResponseList(addressPage.getContent());
        return PageResponse.of(addressResponses,addressPage);
    }
}
