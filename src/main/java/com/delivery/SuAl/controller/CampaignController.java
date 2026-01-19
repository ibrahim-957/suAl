package com.delivery.SuAl.controller;

import com.delivery.SuAl.model.request.marketing.CreateCampaignRequest;
import com.delivery.SuAl.model.request.marketing.GetEligibleCampaignsRequest;
import com.delivery.SuAl.model.request.marketing.UpdateCampaignRequest;
import com.delivery.SuAl.model.request.marketing.ValidateCampaignRequest;
import com.delivery.SuAl.model.response.marketing.CampaignResponse;
import com.delivery.SuAl.model.response.marketing.EligibleCampaignsResponse;
import com.delivery.SuAl.model.response.marketing.ValidateCampaignResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.service.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/api/campaign")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CampaignController {
    private final CampaignService campaignService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new campaign")
    public ResponseEntity<ApiResponse<CampaignResponse>> createCampaign(
            @Valid @RequestPart("createProductRequest") CreateCampaignRequest request,
            @Parameter(description = "Product image file")
            @RequestPart(value = "image", required = false) MultipartFile image) {
        log.info("Creating campaign with code: {}", request.getCampaignCode());

        CampaignResponse response = campaignService.createCampaign(request, image);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Campaign created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CampaignResponse>> getCampaignById(@PathVariable Long id) {
        log.info("Fetching campaign with id: {}", id);

        CampaignResponse response = campaignService.getCampaignById(id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CampaignResponse>>> getAllCampaigns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        log.info("Fetching all campaigns - page: {}, size: {}", page, size);

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<CampaignResponse> campaignPage = campaignService.getCampaigns(pageable);
        PageResponse<CampaignResponse> response = PageResponse.of(campaignPage.getContent(), campaignPage);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CampaignResponse>> updateCampaign(
            @PathVariable Long id,
            @Valid @RequestPart("updateProductRequest") UpdateCampaignRequest request,
            @Parameter(description = "Product image file")
            @RequestPart(value = "image", required = false) MultipartFile image) {
        log.info("Updating campaign with id: {}", id);

        CampaignResponse response = campaignService.updateCampaign(id, request, image);

        return ResponseEntity.ok(ApiResponse.success("Campaign updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCampaign(@PathVariable Long id) {
        log.info("Deleting campaign with id: {}", id);

        campaignService.deleteCampaignById(id);

        return ResponseEntity.ok(ApiResponse.success("Campaign deleted successfully", null));
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<ValidateCampaignResponse>> validateCampaign(
            @Valid @RequestBody ValidateCampaignRequest request) {
        log.info("Validating campaign: {} for user: {}", request.getCampaignCode(), request.getUserId());

        ValidateCampaignResponse response = campaignService.validateCampaign(request);

        if (Boolean.TRUE.equals(response.getIsValid())) {
            return ResponseEntity.ok(ApiResponse.success("Campaign is valid", response));
        } else {
            return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
        }
    }

    @PostMapping("/eligible")
    public ResponseEntity<ApiResponse<EligibleCampaignsResponse>> getEligibleCampaigns(
            @Valid @RequestBody GetEligibleCampaignsRequest request) {
        log.info("Getting eligible campaigns for user: {}", request.getUserId());

        EligibleCampaignsResponse response = campaignService.getEligibleCampaigns(request);

        return ResponseEntity.ok(ApiResponse.success(
                String.format("Found %d eligible campaigns", response.getEligibleCampaigns().size()),
                response
        ));
    }
}