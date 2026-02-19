package com.delivery.SuAl.controller;

import com.delivery.SuAl.model.request.product.CreateProductSizeRequest;
import com.delivery.SuAl.model.request.product.UpdateProductSizeRequest;
import com.delivery.SuAl.model.response.product.ProductSizeResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.service.ProductSizeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/product-sizes")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ProductSizeController {
    private final ProductSizeService productSizeService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductSizeResponse>> create(
            @RequestBody @Valid CreateProductSizeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse
                        .success("Product size created successfully",
                                productSizeService.createProductSize(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductSizeResponse>> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateProductSizeRequest request) {
        return ResponseEntity.ok(ApiResponse
                .success("Product size updated successfully",
                        productSizeService.updateProductSize(id, request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductSizeResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productSizeService.getById(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        productSizeService.deleteProductSize(id);
        return ResponseEntity.ok(ApiResponse
                .success("Product size deleted successfully", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductSizeResponse>>> getAllProducts(
            @ParameterObject Pageable pageable) {

        PageResponse<ProductSizeResponse> response = productSizeService.getAllProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
