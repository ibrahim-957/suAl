package com.delivery.SuAl.controller;

import com.delivery.SuAl.model.request.address.CreateAddressRequest;
import com.delivery.SuAl.model.request.address.UpdateAddressRequest;
import com.delivery.SuAl.model.response.address.AddressResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/api/users/{userId}/addresses")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AddressController {
    private final AddressService addressService;

    @PostMapping("/user")
    public ResponseEntity<AddressResponse> createAddressByUser(
            @RequestHeader("'X-Phone-Number") String phoneNumber,
            @Valid @RequestBody CreateAddressRequest createAddressRequest){
        log.info("POST /v1/api/addresses/user - User with phone {} creating address", phoneNumber);

        AddressResponse response = addressService.createAddressByUser(phoneNumber, createAddressRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/operator")
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @PathVariable Long userId,
            @Valid @RequestBody CreateAddressRequest createAddressRequest
    ) {
        log.info("POST /api/users/{}/addresses - Creating address", userId);
        AddressResponse addressResponse = addressService.createAddress(userId, createAddressRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(addressResponse));
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddressById(
            @PathVariable Long userId,
            @PathVariable Long addressId
    ) {
        log.info("GET /api/users/{}/addresses/{}", userId, addressId);
        AddressResponse addressResponse = addressService.getAddressById(userId, addressId);
        return ResponseEntity.ok(ApiResponse.success(addressResponse));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId,
            @Valid @RequestBody UpdateAddressRequest updateAddressRequest
    ) {
        log.info("PUT /api/users/{}/addresses/{}", userId, addressId);
        AddressResponse addressResponse = addressService.updateAddress(userId, addressId, updateAddressRequest);
        return ResponseEntity.ok(ApiResponse.success("Address updated successfully", addressResponse));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId
    ) {
        log.info("DELETE /api/users/{}/addresses/{} - Deleting address", userId, addressId);
        addressService.deleteAddress(addressId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getUserAddresses(@PathVariable Long userId) {
        log.info("GET /api/users/{}/addresses", userId);
        List<AddressResponse> addressResponseList = addressService.getUserAddresses(userId);
        return ResponseEntity.ok(ApiResponse.success(addressResponseList));
    }
}
