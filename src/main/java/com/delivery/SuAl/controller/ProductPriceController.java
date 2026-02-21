package com.delivery.SuAl.controller;

import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.model.request.product.CreateProductPriceRequest;
import com.delivery.SuAl.model.response.product.ProductPriceResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.service.ProductPriceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/api/product-prices")
@RequiredArgsConstructor
@Slf4j
@Validated
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
public class ProductPriceController {
    private final ProductPriceService productPriceService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductPriceResponse>> createPrice(
            @Valid @RequestBody CreateProductPriceRequest request,
            @AuthenticationPrincipal User user) {
        log.info("POST /v1/api/product-prices - Creating price for product: {}",
                request.getProductId());

        ProductPriceResponse response = productPriceService.createProductPrice(request, user);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product price created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductPriceResponse>> getPriceById(@PathVariable Long id) {
        log.info("GET /v1/api/product-prices/{}", id);

        ProductPriceResponse response = productPriceService.getActivePrice(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/product/{productId}/history")
    public ResponseEntity<ApiResponse<List<ProductPriceResponse>>> getPriceHistory(
            @PathVariable Long productId) {
        log.info("GET /v1/api/product-prices/product/{}/history", productId);

        List<ProductPriceResponse> response =
                productPriceService.getPriceHistory(productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
