package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.model.request.product.CreateProductPriceRequest;
import com.delivery.SuAl.model.response.product.ProductPriceResponse;

import java.util.List;

public interface ProductPriceService {
    ProductPriceResponse createProductPrice(CreateProductPriceRequest request, User createdBy);

    ProductPriceResponse getActivePrice(Long productId);

    List<ProductPriceResponse> getPriceHistory(Long productId);
}
