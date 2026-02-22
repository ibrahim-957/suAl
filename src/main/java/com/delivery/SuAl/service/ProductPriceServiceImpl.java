package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.entity.ProductPrice;
import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.exception.ActivePriceAlreadyExistsException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.mapper.ProductPriceMapper;
import com.delivery.SuAl.model.request.product.CreateProductPriceRequest;
import com.delivery.SuAl.model.response.product.ProductPriceResponse;
import com.delivery.SuAl.repository.ProductPriceRepository;
import com.delivery.SuAl.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductPriceServiceImpl implements ProductPriceService {
    private final ProductPriceRepository productPriceRepository;
    private final ProductRepository productRepository;
    private final ProductPriceMapper productPriceMapper;

    @Override
    @Transactional
    public ProductPriceResponse createProductPrice(CreateProductPriceRequest request, User createdBy) {
        log.info("Creating new price for product ID: {}", request.getProductId());
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + request.getProductId()));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime validFrom = request.getValidFrom() != null ? request.getValidFrom() : now;

        if (productPriceRepository.existsByProductIdAndValidToIsNull(request.getProductId())) {
            int closed = productPriceRepository.closeActivePrice(request.getProductId(), now);
            if (closed == 0){
                throw new ActivePriceAlreadyExistsException(
                        "Failed to close existing active price for product ID: "
                        + request.getProductId()
                        +". Please retry.");
            }
            log.info("Closed active price for product ID: {}", request.getProductId());
        }

        ProductPrice newPrice = productPriceMapper.toEntity(request);
        newPrice.setProduct(product);
        newPrice.setValidFrom(validFrom);
        newPrice.setValidTo(null);
        newPrice.setCreatedBy(createdBy);

        ProductPrice saved = productPriceRepository.save(newPrice);
        log.info("New price created with ID: {} for product ID: {}",
                saved.getId(), request.getProductId());
        return productPriceMapper.toResponse(saved);
    }

    @Override
    public ProductPriceResponse getActivePrice(Long productId) {
        log.info("Getting active price for product ID: {}", productId);

        if (!productRepository.existsById(productId)) {
            throw new NotFoundException("Product not found with id: " + productId);
        }
        return productPriceRepository.findActiveByProductId(productId)
                .map(productPriceMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("No active price found for product ID: " + productId));    }

    @Override
    public List<ProductPriceResponse> getPriceHistory(Long productId) {
        log.info("Getting price history for product ID: {}", productId);
        if (!productRepository.existsById(productId)) {
            throw new NotFoundException("Product not found with id: " + productId);
        }
        return productPriceMapper.toResponseList(
                productPriceRepository.findHistoryByProductId(productId)
        );
    }
}
