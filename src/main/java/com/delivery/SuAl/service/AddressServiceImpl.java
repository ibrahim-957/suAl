package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Address;
import com.delivery.SuAl.entity.Customer;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.mapper.AddressMapper;
import com.delivery.SuAl.model.request.address.CreateAddressByCustomerRequest;
import com.delivery.SuAl.model.request.address.CreateAddressByOperatorRequest;
import com.delivery.SuAl.model.request.address.UpdateAddressRequest;
import com.delivery.SuAl.model.response.address.AddressResponse;
import com.delivery.SuAl.repository.AddressRepository;
import com.delivery.SuAl.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {
    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;
    private final CustomerRepository customerRepository;

    @Override
    public AddressResponse createAddressByCustomer(String phoneNumber, CreateAddressByCustomerRequest request) {
        log.info("Customer create Address");

        Customer customer = customerRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new NotFoundException("Customer not found with phone number: " + phoneNumber));

        Address address = addressMapper.toEntity(request);
        address.setCustomer(customer);

        Address savedAddress = addressRepository.save(address);

        log.info("Address created successfully for phone number: {}", phoneNumber);
        return addressMapper.toResponse(savedAddress);
    }

    @Override
    public AddressResponse createAddress(CreateAddressByOperatorRequest createAddressByOperatorRequest) {
        log.info("Address created for customer by ID {}", createAddressByOperatorRequest.getCustomerId());

        Customer customer = customerRepository.findById(createAddressByOperatorRequest.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Customer not found with id: " + createAddressByOperatorRequest.getCustomerId()));

        Address address = addressMapper.toEntity(createAddressByOperatorRequest);
        address.setCustomer(customer);

        Address savedAddress = addressRepository.save(address);
        log.info("Address created for customer by ID {}", createAddressByOperatorRequest.getCustomerId());

        return addressMapper.toResponse(savedAddress);
    }

    @Override
    public AddressResponse getAddressById(Long customerId, Long addressId) {
        log.info("Fetching address ID: {} for customer by ID {}", addressId, customerId);

        Address address = addressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new NotFoundException("Address not found with id: " + addressId));
        return addressMapper.toResponse(address);
    }

    @Override
    public AddressResponse updateAddress(Long customerId, Long addressId, UpdateAddressRequest updateAddressRequest) {
        log.info("Updating address with ID: {}", addressId);

        Address address = addressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new NotFoundException("Address not found with id: " + addressId));

        addressMapper.updateEntityFromRequest(updateAddressRequest, address);
        Address savedAddress = addressRepository.save(address);
        log.info("Address updated for customer by ID {}", customerId);
        return addressMapper.toResponse(savedAddress);

    }

    @Override
    public void deleteAddress(Long id) {
        log.info("Deleting address with ID: {}", id);
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Address not found with id: " + id));
        address.setIsActive(false);
        addressRepository.save(address);
        log.info("Address deleted successfully with ID: {}", id);
    }

    @Override
    public List<AddressResponse> getCustomerAddresses(Long customerId) {
        log.info("Getting customer address by ID: {}", customerId);

        if (!customerRepository.existsById(customerId))
            throw new NotFoundException("Customer not found with id: " + customerId);

        return addressMapper.toResponseList(addressRepository.findByCustomerId(customerId));
    }
}