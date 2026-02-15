package com.delivery.SuAl.controller;

import com.delivery.SuAl.model.request.affordablepackage.CreateAffordablePackageRequest;
import com.delivery.SuAl.model.request.affordablepackage.UpdateAffordablePackageRequest;
import com.delivery.SuAl.model.response.affordablepackage.AffordablePackageResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.service.AffordablePackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/affordable-packages")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AffordablePackageController {
    private final AffordablePackageService affordablePackageService;

    @PostMapping
    public ResponseEntity<ApiResponse<AffordablePackageResponse>> createPackage(
            @RequestBody @Valid CreateAffordablePackageRequest request
    ) {
        log.info("POST /v1/api/affordable-packages - Creating new affordable package: {}", request.getName());

        AffordablePackageResponse response = affordablePackageService.createPackage(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Package created successfully", response));
    }

    @PutMapping("/{packageId}")
    public ResponseEntity<ApiResponse<AffordablePackageResponse>> updatePackage(
            @PathVariable Long packageId,
            @RequestBody @Valid UpdateAffordablePackageRequest request
    ) {
        log.info("PUT /v1/api/affordable-packages/{} - Updating package", packageId);

        AffordablePackageResponse response = affordablePackageService.updatePackage(packageId, request);
        return ResponseEntity.ok(ApiResponse.success("Package updated successfully", response));
    }

    @GetMapping("/{packageId}")
    public ResponseEntity<ApiResponse<AffordablePackageResponse>> getPackageById(
            @PathVariable Long packageId
    ) {
        log.info("GET /v1/api/affordable-packages/{} - Fetching package", packageId);

        AffordablePackageResponse response = affordablePackageService.getPackageById(packageId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<PageResponse<AffordablePackageResponse>>> getAllActivePackages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        log.info("GET /v1/api/affordable-packages/active - Fetching all active packages");

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, sortDirection, sortBy);

        PageResponse<AffordablePackageResponse> response = affordablePackageService.getAllActivePackages(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AffordablePackageResponse>>> getAllPackages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        log.info("GET /v1/api/affordable-packages - Fetching all packages for management");

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, sortDirection, sortBy);

        PageResponse<AffordablePackageResponse> response = affordablePackageService.getAllPackages(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<ApiResponse<PageResponse<AffordablePackageResponse>>> getPackagesByCompany(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        log.info("GET /v1/api/affordable-packages/company/{} - Fetching packages for company", companyId);

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, sortDirection, sortBy);

        PageResponse<AffordablePackageResponse> response =
                affordablePackageService.getPackagesByCompany(companyId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{packageId}/toggle-status")
    public ResponseEntity<ApiResponse<AffordablePackageResponse>> togglePackageStatus(
            @PathVariable Long packageId,
            @RequestParam boolean isActive
    ) {
        log.info("PATCH /v1/api/affordable-packages/{}/toggle-status - Setting status to: {}",
                packageId, isActive);

        AffordablePackageResponse response = affordablePackageService.togglePackageStatus(packageId, isActive);
        return ResponseEntity.ok(ApiResponse.success("Package status updated successfully", response));
    }

    @DeleteMapping("/{packageId}")
    public ResponseEntity<ApiResponse<Void>> deletePackage(@PathVariable Long packageId) {
        log.info("DELETE /v1/api/affordable-packages/{} - Deleting package", packageId);

        affordablePackageService.deletePackage(packageId);
        return ResponseEntity.ok(ApiResponse.success("Package deleted successfully", null));
    }
}