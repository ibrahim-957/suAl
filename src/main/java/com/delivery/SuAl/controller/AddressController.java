package com.delivery.SuAl.controller;

import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.model.request.address.CreateAddressByCustomerRequest;
import com.delivery.SuAl.model.request.address.CreateAddressByOperatorRequest;
import com.delivery.SuAl.model.request.address.UpdateAddressRequest;
import com.delivery.SuAl.model.response.address.AddressResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/api/customers/addresses")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AddressController {
    private final AddressService addressService;

    @PostMapping("/customer")
    public ResponseEntity<AddressResponse> createAddressByCustomer(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateAddressByCustomerRequest request) {
        log.info("POST /v1/api/addresses/customer - Customer with phone {} creating address", user.getPhoneNumber());

        AddressResponse response = addressService.createAddressByCustomer(user.getPhoneNumber(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/{customerId}/operator")
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @Valid @RequestBody CreateAddressByOperatorRequest createAddressByOperatorRequest
    ) {
        log.info("POST /api/customers/{}/addresses - Creating address", createAddressByOperatorRequest.getCustomerId());
        AddressResponse addressResponse = addressService.createAddress(createAddressByOperatorRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(addressResponse));
    }

    @GetMapping("/{customerId}/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddressById(
            @PathVariable Long customerId,
            @PathVariable Long addressId
    ) {
        log.info("GET /api/customers/{}/addresses/{}", customerId, addressId);
        AddressResponse addressResponse = addressService.getAddressById(customerId, addressId);
        return ResponseEntity.ok(ApiResponse.success(addressResponse));
    }

    @PutMapping("/{customerId}/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @PathVariable Long customerId,
            @PathVariable Long addressId,
            @Valid @RequestBody UpdateAddressRequest updateAddressRequest
    ) {
        log.info("PUT /api/customers/{}/addresses/{}", customerId, addressId);
        AddressResponse addressResponse = addressService.updateAddress(customerId, addressId, updateAddressRequest);
        return ResponseEntity.ok(ApiResponse.success("Address updated successfully", addressResponse));
    }

    @DeleteMapping("/{customerId}/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long customerId,
            @PathVariable Long addressId
    ) {
        log.info("DELETE /api/customers/{}/addresses/{} - Deleting address", customerId, addressId);
        addressService.deleteAddress(addressId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getCustomerAddresses(@PathVariable Long customerId) {
        log.info("GET /api/customers/{}/addresses", customerId);
        List<AddressResponse> addressResponseList = addressService.getCustomerAddresses(customerId);
        return ResponseEntity.ok(ApiResponse.success(addressResponseList));
    }
}
