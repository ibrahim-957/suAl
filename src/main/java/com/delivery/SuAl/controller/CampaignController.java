package com.delivery.SuAl.controller;

import com.delivery.SuAl.model.request.marketing.ApplyCampaignRequest;
import com.delivery.SuAl.model.request.marketing.CreateCampaignRequest;
import com.delivery.SuAl.model.request.marketing.UpdateCampaignRequest;
import com.delivery.SuAl.model.request.marketing.ValidateCampaignRequest;
import com.delivery.SuAl.model.response.marketing.ApplyCampaignResponse;
import com.delivery.SuAl.model.response.marketing.CampaignResponse;
import com.delivery.SuAl.model.response.marketing.ValidateCampaignResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.service.CampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
@RequestMapping("/v1/api/campaign")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CampaignController {
    private final CampaignService campaignService;

    @PostMapping
    public ResponseEntity<ApiResponse<CampaignResponse>> createCampaign(
            @Valid @RequestBody CreateCampaignRequest request) {

        CampaignResponse response = campaignService.createCampaign(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Campaign created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CampaignResponse>> getCampaignById(@PathVariable Long id) {

        CampaignResponse response = campaignService.getCampaignById(id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CampaignResponse>>> getAllCampaigns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<CampaignResponse> campaignPage = campaignService.getCampaigns(pageable);
        PageResponse<CampaignResponse> response = PageResponse.of(campaignPage.getContent(), campaignPage);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CampaignResponse>> updateCampaign(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCampaignRequest request) {

        CampaignResponse response = campaignService.updateCampaign(id, request);

        return ResponseEntity.ok(ApiResponse.success("Campaign updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCampaign(@PathVariable Long id) {

        campaignService.deleteCampaignById(id);

        return ResponseEntity.ok(ApiResponse.success("Campaign deleted successfully", null));
    }

//    @PostMapping("/apply")
//    public ResponseEntity<ApiResponse<ApplyCampaignResponse>> applyCampaign(
//            @Valid @RequestBody ApplyCampaignRequest request) {
//
//        ApplyCampaignResponse response = campaignService.applyCampaign(request);
//
//        return ResponseEntity.ok(ApiResponse.success("Campaign applied successfully", response));
//    }
//
//    @PostMapping("/validate")
//    public ResponseEntity<ApiResponse<ValidateCampaignResponse>> validateCampaign(
//        @Valid @RequestBody ValidateCampaignRequest request
//    ){
//        ValidateCampaignResponse response = campaignService.validateCampaign(request);
//
//        if (response.getIsValid()){
//            return ResponseEntity.ok(ApiResponse.success("Campaign validated successfully", response));
//        } else {
//            return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
//        }
//    }
}