package com.delivery.SuAl.service;

import com.delivery.SuAl.model.request.product.CreatePriceRequest;
import com.delivery.SuAl.model.request.product.UpdatePriceRequest;
import com.delivery.SuAl.model.response.product.PriceResponse;

import java.util.List;
import java.util.Map;

public interface PriceService {
    PriceResponse createPrice(CreatePriceRequest createPriceRequest);

    PriceResponse getPriceById(Long id);

    PriceResponse updatePrice(Long id, UpdatePriceRequest updatePriceRequest);

    void deleteOperator(Long id);

    PriceResponse getPriceByProductId(Long productId);

    Map<Long, PriceResponse> getPricesByProductIds(List<Long> productIds);
}
