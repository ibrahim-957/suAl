package com.delivery.SuAl.service;

import com.delivery.SuAl.model.request.affordablepackage.CreateAffordablePackageRequest;
import com.delivery.SuAl.model.request.affordablepackage.UpdateAffordablePackageRequest;
import com.delivery.SuAl.model.response.affordablepackage.AffordablePackageResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface AffordablePackageService {
    AffordablePackageResponse createPackage(CreateAffordablePackageRequest request, MultipartFile image);

    AffordablePackageResponse updatePackage(Long packageId, UpdateAffordablePackageRequest request, MultipartFile image);

    AffordablePackageResponse getPackageById(Long packageId);

    PageResponse<AffordablePackageResponse> getAllActivePackages(Pageable pageable);

    PageResponse<AffordablePackageResponse> getAllPackages(Pageable pageable);

    PageResponse<AffordablePackageResponse> getPackagesByCompany(Long companyId, Pageable pageable);

    AffordablePackageResponse togglePackageStatus(Long packageId, boolean isActive);

    void deletePackage(Long packageId);
}
