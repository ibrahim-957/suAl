package com.delivery.SuAl.service;

import com.delivery.SuAl.model.request.basket.CreateOrderFromBasketRequest;
import com.delivery.SuAl.model.request.order.CreateOrderRequest;
import com.delivery.SuAl.model.response.basket.BasketCalculationResponse;
import com.delivery.SuAl.model.response.basket.BasketResponse;

public interface BasketService {
    BasketResponse getOrCreateBasket(Long userId);

    BasketResponse getBasket(Long userId);

    BasketResponse addItem(Long userId, Long productId, Integer quantity);

    BasketResponse updateItem(Long userId, Long basketItemId, Integer quantity);

    BasketResponse removeItem(Long userId, Long basketItemId);

    void clearBasket(Long userId);

    BasketCalculationResponse calculateBasket(Long userId);

    BasketCalculationResponse calculateBasketWithPromo(Long userId, String promoCode);

    CreateOrderRequest convertBasketToOrderRequest(Long userId, CreateOrderFromBasketRequest request);
}