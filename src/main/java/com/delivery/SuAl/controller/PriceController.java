package com.delivery.SuAl.controller;

import com.delivery.SuAl.model.request.product.CreatePriceRequest;
import com.delivery.SuAl.model.request.product.UpdatePriceRequest;
import com.delivery.SuAl.model.response.product.PriceResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.service.PriceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
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
@RequestMapping("/v1/api/prices")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PriceController {
//    private final PriceService priceService;
//
//    @PostMapping
//    public ResponseEntity<ApiResponse<PriceResponse>> createPrice(
//            @Valid @RequestBody CreatePriceRequest createPriceRequest) {
//        PriceResponse priceResponse = priceService.createPrice(createPriceRequest);
//        return ResponseEntity
//                .status(HttpStatus.CREATED)
//                .body(ApiResponse.success("Price created successfully", priceResponse));
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<ApiResponse<PriceResponse>> getPriceById(@PathVariable Long id) {
//        PriceResponse priceResponse = priceService.getPriceById(id);
//        return ResponseEntity.ok(ApiResponse.success("Price found successfully", priceResponse));
//    }
//
//    @GetMapping("/product/{productId}")
//    public ResponseEntity<ApiResponse<PriceResponse>> getPriceByProductId(@PathVariable Long productId) {
//        PriceResponse priceResponse = priceService.getPriceByProductId(productId);
//        return ResponseEntity.ok(ApiResponse.success("Price found successfully", priceResponse));
//    }
//
//    @PutMapping("/{id}")
//    public ResponseEntity<ApiResponse<PriceResponse>> updatePrice(
//            @PathVariable Long id,
//            @Valid @RequestBody UpdatePriceRequest updatePriceRequest){
//        PriceResponse priceResponse = priceService.updatePrice(id, updatePriceRequest);
//        return ResponseEntity.ok(ApiResponse.success("Price updated successfully", priceResponse));
//    }
//
//    @DeleteMapping("/{id}")
//    public ResponseEntity<ApiResponse<PriceResponse>> deletePrice(@PathVariable Long id){
//        priceService.deleteOperator(id);
//        return ResponseEntity.ok(ApiResponse.success("Price deleted successfully", null));
//    }
}
