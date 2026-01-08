package com.delivery.SuAl.controller;

import com.delivery.SuAl.model.request.cart.CalculatePriceRequest;
import com.delivery.SuAl.model.response.cart.CartCalculationResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.service.CartPriceCalculationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/cart")
@Slf4j
@RequiredArgsConstructor
@Validated
public class CartController {
    private final CartPriceCalculationService cartPriceCalculationService;

    @PostMapping
    public ResponseEntity<ApiResponse<CartCalculationResponse>> calculatePrice(
            @RequestBody @Valid CalculatePriceRequest request){
        log.info("Price calculation request from user: {}, items: {}", request.getUserId(), request.getItems().size());
        CartCalculationResponse response = cartPriceCalculationService.calculatePrice(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
