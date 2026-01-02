package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Address;
import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.mapper.AddressMapper;
import com.delivery.SuAl.model.request.address.CreateAddressRequest;
import com.delivery.SuAl.model.request.address.UpdateAddressRequest;
import com.delivery.SuAl.model.response.address.AddressResponse;
import com.delivery.SuAl.repository.AddressRepository;
import com.delivery.SuAl.repository.UserRepository;
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
    private final UserRepository userRepository;

    @Override
    public AddressResponse createAddressByUser(String phoneNumber, CreateAddressRequest request) {
        log.info("User create Address");

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new NotFoundException("User not found with phone number: " + phoneNumber));

        Address address = addressMapper.toEntity(request);
        address.setUser(user);
        address.setIsActive(true);

        Address savedAddress = addressRepository.save(address);

        log.info("Address created successfully for phone number: {}", phoneNumber);
        return addressMapper.toResponse(savedAddress);
    }

    @Override
    public AddressResponse createAddress(Long userId, CreateAddressRequest createAddressRequest) {
        log.info("Address created for user by ID {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

        Address address = addressMapper.toEntity(createAddressRequest);
        address.setUser(user);
        address.setIsActive(true);

        Address savedAddress = addressRepository.save(address);
        log.info("Address created for user by ID {}", userId);

        return addressMapper.toResponse(savedAddress);
    }

    @Override
    public AddressResponse getAddressById(Long userId, Long addressId) {
        log.info("Fetching address ID: {} for user by ID {}", addressId, userId);

        Address address = addressRepository.findByIdAndUserIdAndIsActiveTrue(userId, addressId)
                .orElseThrow(() -> new NotFoundException("Address not found with ID " + addressId));
        return addressMapper.toResponse(address);
    }

    @Override
    public AddressResponse updateAddress(Long userId, Long addressId, UpdateAddressRequest updateAddressRequest) {
        log.info("Updating address with ID: {}", addressId);

        Address address = addressRepository.findByIdAndUserIdAndIsActiveTrue(addressId, userId)
                .orElseThrow(() -> new NotFoundException("Address not found with ID " + addressId));

        addressMapper.updateEntityFromRequest(updateAddressRequest, address);
        Address savedAddress = addressRepository.save(address);
        log.info("Address updated for user by ID {}", userId);
        return addressMapper.toResponse(savedAddress);
    }

    @Override
    public void deleteAddress(Long id) {
        log.info("Deleting address with ID: {}", id);
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Address not found with ID " + id));
        address.setIsActive(false);
        addressRepository.save(address);
        log.info("Address deleted successfully with ID: {}", id);
    }

    @Override
    public List<AddressResponse> getUserAddresses(Long userId) {
        log.info("Getting user address by ID: {}", userId);

        if (!userRepository.existsById(userId))
            throw new NotFoundException("User not found with ID: " + userId);

        return addressMapper.toResponseList(addressRepository.findByUserId(userId));
    }
}
