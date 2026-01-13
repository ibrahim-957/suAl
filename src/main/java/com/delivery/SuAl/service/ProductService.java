package com.delivery.SuAl.service;

import com.delivery.SuAl.model.request.product.CreateProductRequest;
import com.delivery.SuAl.model.request.product.UpdateProductRequest;
import com.delivery.SuAl.model.response.product.ProductResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.ProductRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface ProductService {
    ProductResponse createProduct(CreateProductRequest request, MultipartFile image);

    ProductResponse getProductByID(Long id);

    ProductResponse updateProduct(Long id, UpdateProductRequest request, MultipartFile image);

    void deleteProductByID(Long id);

    PageResponse<ProductResponse> getAllProducts(Pageable pageable);
}
