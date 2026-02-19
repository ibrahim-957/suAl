package com.delivery.SuAl.service;

import com.delivery.SuAl.model.request.product.CreateProductSizeRequest;
import com.delivery.SuAl.model.request.product.UpdateProductSizeRequest;
import com.delivery.SuAl.model.response.product.ProductSizeResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import org.springframework.data.domain.Pageable;

public interface ProductSizeService {
    ProductSizeResponse createProductSize(CreateProductSizeRequest request);
    ProductSizeResponse updateProductSize(Long id, UpdateProductSizeRequest request);
    ProductSizeResponse getById(Long id);
    void deleteProductSize(Long id);
    PageResponse<ProductSizeResponse> getAllProducts(Pageable pageable);
}
