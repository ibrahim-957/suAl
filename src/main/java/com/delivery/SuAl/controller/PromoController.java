package com.delivery.SuAl.controller;

import com.delivery.SuAl.model.request.marketing.ApplyPromoRequest;
import com.delivery.SuAl.model.request.marketing.CreatePromoRequest;
import com.delivery.SuAl.model.request.marketing.UpdatePromoRequest;
import com.delivery.SuAl.model.request.marketing.ValidatePromoRequest;
import com.delivery.SuAl.model.response.marketing.ApplyPromoResponse;
import com.delivery.SuAl.model.response.marketing.PromoResponse;
import com.delivery.SuAl.model.response.marketing.ValidatePromoResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.service.PromoService;
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
@RequestMapping("/v1/api/promos")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PromoController {
    private final PromoService promoService;

    @PostMapping
    public ResponseEntity<ApiResponse<PromoResponse>> createPromo(
            @Valid @RequestBody CreatePromoRequest request
            ){
        PromoResponse response = promoService.createPromo(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Promo created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PromoResponse>> getPromoById(@PathVariable Long id) {

        PromoResponse response = promoService.getPromoById(id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PromoResponse>>> getAllPromos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<PromoResponse> promoPage = promoService.getAllPromos(pageable);
        PageResponse<PromoResponse> response = PageResponse.of(promoPage.getContent(), promoPage);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PromoResponse>> updatePromo(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePromoRequest request) {

        PromoResponse response = promoService.updatePromo(id, request);

        return ResponseEntity.ok(ApiResponse.success("Promo updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePromo(@PathVariable Long id) {

        promoService.deletePromoById(id);

        return ResponseEntity.ok(ApiResponse.success("Promo deleted successfully", null));
    }

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<ApplyPromoResponse>> applyPromo(
            @Valid @RequestBody ApplyPromoRequest request) {

        ApplyPromoResponse response = promoService.applyPromo(request);

        return ResponseEntity.ok(ApiResponse.success("Promo applied successfully", response));
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<PromoResponse>> validatePromo(
        @Valid @RequestBody ValidatePromoRequest request
    ){
        ValidatePromoResponse response = promoService.validatePromo(request);

        if (response.getIsValid()) {
            return ResponseEntity.ok(ApiResponse.success("Promo is valid", response.getPromoResponse()));
        } else {
            return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response.getPromoResponse()));
        }
    }
}