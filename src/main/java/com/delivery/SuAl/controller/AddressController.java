package com.delivery.SuAl.controller;

import com.delivery.SuAl.model.request.address.CreateAddressRequest;
import com.delivery.SuAl.model.request.address.UpdateAddressRequest;
import com.delivery.SuAl.model.response.address.AddressResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/addresses")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AddressController {
    private final AddressService addressService;

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
          @Valid @RequestBody CreateAddressRequest createAddressRequest){

        AddressResponse addressResponse = addressService.createAddress(createAddressRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(addressResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddressById(@PathVariable Long id){
        AddressResponse addressResponse = addressService.getAddressById(id);
        return ResponseEntity.ok(ApiResponse.success(addressResponse));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAddressRequest updateAddressRequest){
        AddressResponse addressResponse = addressService.updateAddress(id, updateAddressRequest);
        return ResponseEntity.ok(ApiResponse.success("Address updated successfully", addressResponse));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> deleteAddress(@PathVariable Long id){
        addressService.deleteAddress(id);
        return ResponseEntity.ok(ApiResponse.success("Address deleted successfully", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AddressResponse>>> getAllAddresses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction){

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable  pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        PageResponse<AddressResponse> pageResponse = addressService.getAllAddresses(pageable);
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }
}
