package com.delivery.SuAl.service;

import com.delivery.SuAl.model.request.cart.CalculatePriceRequest;
import com.delivery.SuAl.model.response.cart.CartCalculationResponse;

public interface CartPriceCalculationService {
    CartCalculationResponse calculatePrice(CalculatePriceRequest request);
}
